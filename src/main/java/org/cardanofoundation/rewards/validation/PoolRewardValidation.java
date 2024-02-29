package org.cardanofoundation.rewards.validation;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.stream.Collectors.toCollection;
import static org.cardanofoundation.rewards.calculation.PoolRewardsCalculation.calculatePoolRewardInEpoch;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.*;

@Slf4j
public class PoolRewardValidation {

    public static PoolRewardCalculationResult computePoolRewardInEpoch(String poolId, int epoch, ProtocolParameters protocolParameters,
                                                                       Epoch epochInfo, BigInteger stakePoolRewardsPot,
                                                                       BigInteger adaInCirculation, PoolHistory poolHistoryCurrentEpoch,
                                                                       HashSet<String> accountDeregistrations, HashSet<String> lateAccountDeregistrations,
                                                                       HashSet<String> accountsRegisteredInThePast,
                                                                       HashSet<String> poolIdsWithSharedRewardAddresses) {
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

        HashSet<String> delegatorDeregistrations = accountDeregistrations.stream()
                .filter(stakeAddresses::contains).collect(toCollection(HashSet::new));

        // There was a different behavior in the previous version of the node
        // If a pool reward address had been used for multiple pools,
        // the stake account only received the reward for one of those pools
        // This is not the case anymore and the stake account receives the reward for all pools
        // Until the Allegra hard fork, this method will be used to emulate the old behavior
        boolean ignoreLeaderReward = false;

        if (epoch < MAINNET_ALLEGRA_HARDFORK_EPOCH) {
            ignoreLeaderReward = poolIdsWithSharedRewardAddresses.contains(poolId);
        }

        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"
        return calculatePoolRewardInEpoch(poolId, poolHistoryCurrentEpoch,
                totalBlocksInEpoch, protocolParameters,
                adaInCirculation, activeStakeInEpoch, stakePoolRewardsPot,
                poolHistoryCurrentEpoch.getOwnerActiveStake(), poolHistoryCurrentEpoch.getOwners(),
                delegatorDeregistrations, ignoreLeaderReward, lateAccountDeregistrations,
                accountsRegisteredInThePast);

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

        BigInteger totalRewardPot = TreasuryValidation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserves, totalFeesForCurrentEpoch);

        BigInteger stakePoolRewardsPot = totalRewardPot.subtract(floor(multiply(totalRewardPot, treasuryGrowRate)));

        PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch);

        HashSet<String> accountDeregistrations = dataProvider.getDeregisteredAccountsInEpoch(epoch + 1, RANDOMNESS_STABILISATION_WINDOW);
        HashSet<String> lateAccountDeregistrations = dataProvider.getLateAccountDeregistrationsInEpoch(epoch + 1, RANDOMNESS_STABILISATION_WINDOW);
        HashSet<String> sharedPoolRewardAddressesWithoutReward = dataProvider.findSharedPoolRewardAddressWithoutReward(epoch);
        HashSet<String> accountsRegisteredInThePast = dataProvider.getRegisteredAccountsUntilLastEpoch(epoch, new HashSet<>(List.of(poolHistoryCurrentEpoch.getRewardAddress())), RANDOMNESS_STABILISATION_WINDOW);

        return computePoolRewardInEpoch(poolId, epoch, protocolParameters, epochInfo, stakePoolRewardsPot, adaInCirculation, poolHistoryCurrentEpoch,
                accountDeregistrations, lateAccountDeregistrations, accountsRegisteredInThePast, sharedPoolRewardAddressesWithoutReward);
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
        HashSet<Reward> delegatorStakeAddresses = new HashSet<>(poolRewardCalculationResult.getMemberRewards());
        for (Reward reward : actualPoolRewardsInEpoch) {
            Reward memberReward = delegatorStakeAddresses.stream()
                    .filter(member -> member.getStakeAddress().equals(reward.getStakeAddress()))
                    .findFirst()
                    .orElse(null);
            if (memberReward == null) {
                log.info("Member reward not found for stake address: " + reward.getStakeAddress());
                log.info("[" + rewardIndex + "] The expected member " + reward.getStakeAddress() + " reward would be : " + reward.getAmount().longValue() + " Lovelace");
                totalDifference = totalDifference.add(reward.getAmount());
            } else {
                BigInteger difference = reward.getAmount().subtract(memberReward.getAmount()).abs();

                if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(reward.getStakeAddress())) {
                    BigInteger poolOwnerReward = poolRewardCalculationResult.getOperatorReward();
                    difference = reward.getAmount().subtract(poolOwnerReward).abs();
                }
                totalDifference = totalDifference.add(difference);

                if (difference.compareTo(BigInteger.ZERO) > 0) {
                    log.info("[" + rewardIndex + "] The difference between expected member " + reward.getStakeAddress() + " reward and actual member reward is : " + reward.getAmount().longValue() + " Lovelace");
                }
            }
            rewardIndex++;
        }

        if (isHigher(totalDifference, BigInteger.ZERO)) {
            log.info("Total difference: " + totalDifference.longValue() + " Lovelace");
        }

        BigInteger totalNoReward = BigInteger.ZERO;
        BigInteger coOwnerReward = BigInteger.ZERO;
        BigInteger calculatedMemberRewards = BigInteger.ZERO;

        HashSet<Reward> actualRewards = new HashSet<>(actualPoolRewardsInEpoch);

        if (poolRewardCalculationResult.getMemberRewards() != null) {
            for (Reward memberReward : poolRewardCalculationResult.getMemberRewards()) {
                Reward actualReward = actualRewards.stream()
                        .filter(reward -> reward.getStakeAddress().equals(memberReward.getStakeAddress()))
                        .findFirst()
                        .orElse(null);
                if (actualReward == null && isHigher(memberReward.getAmount(), BigInteger.ZERO) && !poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                    totalNoReward = totalNoReward.add(memberReward.getAmount());
                    log.info("No reward! The difference between expected member " + memberReward.getStakeAddress() + " reward and actual member reward is : " + memberReward.getAmount().longValue() + " Lovelace");
                }

                // Co-owner reward is not included in the member rewards and would be added to the reward address of the pool
                if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                    coOwnerReward = coOwnerReward.add(memberReward.getAmount());
                }
            }

            if (isHigher(totalNoReward, BigInteger.ZERO)) {
                log.info("Total no reward: " + totalNoReward.longValue() + " Lovelace");
            }

            calculatedMemberRewards = poolRewardCalculationResult.getMemberRewards().stream().map(Reward::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
        }

        boolean isValid = actualPoolReward
                .subtract(calculatedMemberRewards.subtract(coOwnerReward)
                        .add(poolRewardCalculationResult.getOperatorReward())).equals(BigInteger.ZERO) &&
                actualPoolReward.subtract(poolRewardCalculationResult.getDistributedPoolReward()).equals(BigInteger.ZERO);

        if (!isValid) {
            log.info("The difference between expected pool reward and actual pool reward is : " + actualPoolReward.subtract(poolRewardCalculationResult.getDistributedPoolReward()).longValue() + " Lovelace");
        }

        return isValid;
    }
}
