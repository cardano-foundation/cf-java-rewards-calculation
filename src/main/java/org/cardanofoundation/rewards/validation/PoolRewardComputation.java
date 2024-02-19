package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.entity.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.LatestStakeAccountUpdate;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.cardanofoundation.rewards.calculation.PoolRewardsCalculation.calculatePoolRewardInEpoch;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.*;
import static org.cardanofoundation.rewards.calculation.util.CurrencyConverter.lovelaceToAda;

public class PoolRewardComputation {

    public static PoolRewardCalculationResult computePoolRewardInEpoch(String poolId, int epoch, ProtocolParameters protocolParameters,
                                                                       Epoch epochInfo, BigInteger stakePoolRewardsPot,
                                                                       BigInteger adaInCirculation, PoolHistory poolHistoryCurrentEpoch,
                                                                       List<LatestStakeAccountUpdate> accountUpdates) {
        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210

        if(poolHistoryCurrentEpoch == null) {
            return PoolRewardCalculationResult.builder().poolId(poolId).epoch(epoch).poolReward(BigInteger.ZERO).build();
        }

        BigInteger activeStakeInEpoch = BigInteger.ZERO;
        if (epochInfo.getActiveStake() != null) {
            activeStakeInEpoch = epochInfo.getActiveStake();
        }

        int totalBlocksInEpoch = epochInfo.getBlockCount();

        if (epoch > 212 && epoch < 255) {
            totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
        }

        // Step 10 a: Check if pool reward address or member stake addresses have been unregistered before
        List<String> stakeAddresses = new ArrayList<>();
        stakeAddresses.add(poolHistoryCurrentEpoch.getRewardAddress());
        stakeAddresses.addAll(poolHistoryCurrentEpoch.getDelegators().stream().map(Delegator::getStakeAddress).toList());

        //List<AccountUpdate> accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(stakeAddresses, epoch - 1);

        accountUpdates = accountUpdates.stream()
                .filter(update -> stakeAddresses.contains(update.getStakeAddress())).toList();

        BigInteger poolOperatorRewardOutlier = correctOutliers(poolId, epoch);

        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"
        return calculatePoolRewardInEpoch(poolId, poolHistoryCurrentEpoch,
                totalBlocksInEpoch, protocolParameters,
                adaInCirculation, activeStakeInEpoch, stakePoolRewardsPot,
                poolHistoryCurrentEpoch.getOwnerActiveStake(), poolHistoryCurrentEpoch.getOwners(),
                accountUpdates, poolOperatorRewardOutlier);

    }

    public static PoolRewardCalculationResult computePoolRewardInEpoch(String poolId, int epoch, DataProvider dataProvider) {
        // Get Epoch information of current epoch
        // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
        Epoch epochInfo = dataProvider.getEpochInfo(epoch);

        // The Shelley era and the ada pot system started on mainnet in epoch 208.
        // Fee and treasury values are 0 for epoch 208.
        BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
        if (epoch > 209) {
            totalFeesForCurrentEpoch = epochInfo.getFees();
        }

        int totalBlocksInEpoch = epochInfo.getBlockCount();

        if (epoch > 212 && epoch < 255) {
            totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
        }

        // Get the ada reserves for the next epoch because it was already updated (int the previous step)
        AdaPots adaPotsForNextEpoch = dataProvider.getAdaPotsForEpoch(epoch + 1);
        BigInteger reserves = adaPotsForNextEpoch.getReserves();

        // Get total ada in circulation
        BigInteger adaInCirculation = TOTAL_LOVELACE.subtract(reserves);

        // Get protocol parameters for current epoch
        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch);

        // Calculate total available reward for pools (total reward pot after treasury cut)
        double decentralizationParameter = protocolParameters.getDecentralisation();
        double monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
        double treasuryGrowRate = protocolParameters.getTreasuryGrowRate();

        BigInteger totalRewardPot = TreasuryComputation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserves, totalFeesForCurrentEpoch);

        BigInteger stakePoolRewardsPot = totalRewardPot.subtract(floor(multiply(totalRewardPot, treasuryGrowRate)));

        PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch);

        List<String> stakeAddresses = new ArrayList<>(poolHistoryCurrentEpoch.getDelegators().stream().map(Delegator::getStakeAddress).toList());
        stakeAddresses.add(poolHistoryCurrentEpoch.getRewardAddress());

        List<LatestStakeAccountUpdate> accountUpdates = dataProvider.getLatestStakeAccountUpdates(epoch, stakeAddresses);
        return computePoolRewardInEpoch(poolId, epoch, protocolParameters, epochInfo, stakePoolRewardsPot, adaInCirculation, poolHistoryCurrentEpoch, accountUpdates);
    }

    /*
        TODO:   Replace this method with the jpa repository call to find reward address owning
                multiple pools that produced blocks in the same epoch
     */
    public static BigInteger correctOutliers(String poolId, int epoch) {
        BigInteger correction = BigInteger.ZERO;

        if (epoch == 212 && poolId.equals("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")) {
            /*
             * The reward_address of pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m is also the
             * reward_address of pool1gh4cj5h5glk5992d0wtela324htr0cn8ujvg53pmuds9guxgz2u. Both pools produced
             * blocks in epoch 214. In a previous node version this caused an outlier where the
             * leader rewards of pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m has been set to 0.
             *
             * This behavior has been changed later so that the owner would receive leader rewards for both pools.
             * Affected reward addresses have been paid out due to a MIR certificate afterward.
             */
            correction = new BigInteger(String.valueOf(-814592210));
        } else if (epoch == 212 && poolId.equals("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")) {
            // pool1qvvn2l690zm3v2p0f3vd66ly6cfs2wjqx34zpqcx5pwsx3eprtp also produced blocks in epoch 214
            // with the same reward address
            correction = new BigInteger(String.valueOf(-669930045));
        } else if (epoch == 213 && poolId.equals("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")) {
            // pool1qvvn2l690zm3v2p0f3vd66ly6cfs2wjqx34zpqcx5pwsx3eprtp also produced blocks in epoch 215
            // with the same reward address
            correction = new BigInteger(String.valueOf(-634057195));
        } else if (epoch == 213 && poolId.equals("pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta")) {
            // pool12crd62rxj8yryvshmgwkxza7um3uhaypwdjeel98lnkf529qdw5 &
            // pool1v4adhelnswa7pwv2njn5h84atw08mlc79ll2ewl2kgxhv3cqwql also produced blocks in epoch 215
            // with the same reward address
            correction = new BigInteger(String.valueOf(-369216376));
        }

        return correction;
    }

    public static boolean poolRewardIsValid(PoolRewardCalculationResult poolRewardCalculationResult, DataProvider dataProvider) {
        int epoch  = poolRewardCalculationResult.getEpoch();
        List<Reward> memberRewardsInEpoch = dataProvider.getMemberRewardsInEpoch(epoch);
        List<TotalPoolRewards> totalPoolRewards = dataProvider.getSumOfMemberAndLeaderRewardsInEpoch(epoch);
        return poolRewardIsValid(poolRewardCalculationResult, memberRewardsInEpoch, totalPoolRewards);
    }

    public static boolean poolRewardIsValid(PoolRewardCalculationResult poolRewardCalculationResult, List<Reward> memberRewardsInEpoch, List<TotalPoolRewards> totalPoolRewards) {
        String poolId = poolRewardCalculationResult.getPoolId();

        BigInteger actualPoolReward = totalPoolRewards.stream()
                .filter(reward -> reward.getPoolId().equals(poolId))
                .map(TotalPoolRewards::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
        List<Reward> actualPoolRewardsInEpoch = memberRewardsInEpoch.stream()
                .filter(reward -> reward.getPoolId().equals(poolId))
                .toList();

        if (actualPoolReward.equals(BigInteger.ZERO)) {
            return poolRewardCalculationResult.getPoolReward().equals(BigInteger.ZERO);
        }

        BigInteger totalDifference = BigInteger.ZERO;
        int rewardIndex = 0;
        for (Reward reward : actualPoolRewardsInEpoch) {
            Reward memberReward = poolRewardCalculationResult.getMemberRewards().stream()
                    .filter(member -> member.getStakeAddress().equals(reward.getStakeAddress()))
                    .findFirst()
                    .orElse(null);
            if (memberReward == null) {
                System.out.println("Member reward not found for stake address: " + reward.getStakeAddress());
                return false;
            }
            BigInteger difference = reward.getAmount().subtract(memberReward.getAmount()).abs();

            if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(reward.getStakeAddress())) {
                BigInteger poolOwnerReward = poolRewardCalculationResult.getOperatorReward();
                difference = reward.getAmount().subtract(poolOwnerReward).abs();
            }
            totalDifference = totalDifference.add(difference);

            if (difference.compareTo(BigInteger.ZERO) > 0) {
                System.out.println("[" + rewardIndex + "] The difference between expected member " + reward.getStakeAddress() + " reward and actual member reward is : " + lovelaceToAda(difference.intValue()) + " ADA");
            }
            rewardIndex++;
        }

        System.out.println("Total difference: " + lovelaceToAda(totalDifference.intValue()) + " ADA");

        BigInteger totalNoReward = BigInteger.ZERO;
        BigInteger coOwnerReward = BigInteger.ZERO;
        BigInteger calculatedMemberRewards = BigInteger.ZERO;

        if (poolRewardCalculationResult.getMemberRewards() != null) {
            for (Reward memberReward : poolRewardCalculationResult.getMemberRewards()) {
                Reward actualReward = actualPoolRewardsInEpoch.stream()
                        .filter(reward -> reward.getStakeAddress().equals(memberReward.getStakeAddress()))
                        .findFirst()
                        .orElse(null);
                if (actualReward == null && isHigher(memberReward.getAmount(), BigInteger.ZERO) && !poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                    totalNoReward = totalNoReward.add(memberReward.getAmount());
                    System.out.println("No reward! The difference between expected member " + memberReward.getStakeAddress() + " reward and actual member reward is : " + lovelaceToAda(memberReward.getAmount().intValue()) + " ADA");
                }

                // Co-owner reward is not included in the member rewards and would be added to the reward address of the pool
                if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                    coOwnerReward = coOwnerReward.add(memberReward.getAmount());
                }
            }
            System.out.println("Total no reward: " + lovelaceToAda(totalNoReward.intValue()) + " ADA");

            calculatedMemberRewards = poolRewardCalculationResult.getMemberRewards().stream().map(Reward::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
        }

        return actualPoolReward
                .subtract(calculatedMemberRewards.subtract(coOwnerReward)
                        .add(poolRewardCalculationResult.getOperatorReward())).equals(BigInteger.ZERO) &&
                actualPoolReward.subtract(poolRewardCalculationResult.getDistributedPoolReward()).equals(BigInteger.ZERO);
    }
}
