package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;
import org.cardanofoundation.rewards.enums.MirPot;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.BigNumberUtils.*;

public class EpochCalculation {

    public static EpochCalculationResult calculateEpochPots(int epoch, DataProvider dataProvider) {
        EpochCalculationResult epochCalculationResult = EpochCalculationResult.builder().epoch(epoch).build();

        double treasuryGrowthRate = 0.2;
        double monetaryExpandRate = 0.003;
        double decentralizationParameter = 1;

        AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);

        BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
        int totalBlocksInEpoch = 0;

        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
        Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);

        /* We need to use the epoch info 2 epochs before as shelley starts in epoch 208 it will be possible to get
           those values from epoch 210 onwards. Before that we need to use the genesis values, but they are not
           needed anyway if the decentralization parameter is > 0.8.
           See: shelley-delegation.pdf 5.4.3 */
        if (epoch > 209) {
            totalFeesForCurrentEpoch = epochInfo.getFees();
            treasuryGrowthRate = protocolParameters.getTreasuryGrowRate();
            monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
            decentralizationParameter = protocolParameters.getDecentralisation();

            totalBlocksInEpoch = epochInfo.getBlockCount();

            if (decentralizationParameter < 0.8 && decentralizationParameter > 0.0) {
                totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
            }
        }

        BigInteger reserveInPreviousEpoch = adaPotsForPreviousEpoch.getReserves();

        BigInteger treasuryInPreviousEpoch = adaPotsForPreviousEpoch.getTreasury();
        BigInteger rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

        BigInteger treasuryCut = multiplyAndFloor(rewardPot, treasuryGrowthRate);
        BigInteger treasuryForCurrentEpoch = treasuryInPreviousEpoch.add(treasuryCut);
        BigInteger stakePoolRewardsPot = rewardPot.subtract(treasuryCut);

        // The sum of all the refunds attached to unregistered reward accounts are added to the
        // treasury (see: Pool Reap Transition, p.53, figure 40, shely-ledger.pdf)
        List<PoolDeregistration> retiredPools = dataProvider.getRetiredPoolsInEpoch(epoch);

        if (retiredPools.size() > 0) {
            // The deposit will pay back one epoch later
            List<AccountUpdate> accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(
                    retiredPools.stream().map(PoolDeregistration::getRewardAddress).toList(), epoch - 1);

            treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(
                    TreasuryCalculation.calculateUnclaimedRefundsForRetiredPools(retiredPools, accountUpdates));
        }
        // Check if there was a MIR Certificate in the previous epoch
        BigInteger treasuryWithdrawals = BigInteger.ZERO;
        List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
        for (MirCertificate mirCertificate : mirCertificates) {
            if (mirCertificate.getPot() == MirPot.TREASURY) {
                treasuryWithdrawals = treasuryWithdrawals.add(mirCertificate.getTotalRewards());
            }
        }
        treasuryForCurrentEpoch = treasuryForCurrentEpoch.subtract(treasuryWithdrawals);

        List<String> poolIds = dataProvider.getPoolsThatProducedBlocksInEpoch(epoch - 2);
        BigInteger totalDistributedRewards = BigInteger.ZERO;

        BigInteger adaInCirculation = TOTAL_LOVELACE.subtract(reserveInPreviousEpoch);
        int optimalPoolCount = protocolParameters.getOptimalPoolCount();
        double influenceParam = protocolParameters.getPoolOwnerInfluence();

        List<PoolRewardCalculationResult> PoolRewardCalculationResults = new ArrayList<>();

        for (int i = 0; i < poolIds.size(); i++) {
            String poolId = poolIds.get(i);

            List<Reward> actualPoolRewardsInEpoch = dataProvider.getRewardListForPoolInEpoch(epoch - 2, poolId);
            PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculationResult.builder()
                    .epoch(epoch)
                    .poolId(poolId)
                    .poolReward(BigInteger.ZERO)
                    .build();

            PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch - 2);
            if(poolHistoryCurrentEpoch != null) {
                BigInteger poolStake = poolHistoryCurrentEpoch.getActiveStake();
                BigInteger poolFees = poolHistoryCurrentEpoch.getPoolFees();
                double poolMargin = poolHistoryCurrentEpoch.getMargin();
                double poolFixedCost = poolHistoryCurrentEpoch.getFixedCost();
                int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCount();

                poolRewardCalculationResult.setPoolFee(poolFees);
                poolRewardCalculationResult.setPoolMargin(poolMargin);
                poolRewardCalculationResult.setPoolCost(poolFixedCost);
                poolRewardCalculationResult.setRewardAddress(poolHistoryCurrentEpoch.getRewardAddress());

                if (blocksPoolHasMinted > 0) {
                    BigInteger activeStakeInEpoch = BigInteger.ZERO;
                    if (epochInfo.getActiveStake() != null) {
                        activeStakeInEpoch = epochInfo.getActiveStake();
                    }

                    // Step 5: Calculate apparent pool performance
                    BigDecimal apparentPoolPerformance =
                            PoolRewardCalculation.calculateApparentPoolPerformance(poolStake,
                                    activeStakeInEpoch,
                                    blocksPoolHasMinted, totalBlocksInEpoch, decentralizationParameter);
                    poolRewardCalculationResult.setApparentPoolPerformance(apparentPoolPerformance);
                    // Step 6: Calculate total available reward for pools (total reward pot after treasury cut)
                    // -----
                    poolRewardCalculationResult.setStakePoolRewardsPot(stakePoolRewardsPot);
                    // shelley-delegation.pdf 5.5.3
                    //      "[...]the relative stake of the pool owner(s) (the amount of ada
                    //      pledged during pool registration)"

                    // Step 7: Get the latest pool update before this epoch and extract the pledge
                    double poolPledge = dataProvider.getPoolPledgeInEpoch(poolId, epoch - 2);

                    PoolOwnerHistory poolOwnersHistoryInEpoch = dataProvider.getHistoryOfPoolOwnersInEpoch(poolId, epoch - 2);
                    BigInteger totalActiveStakeOfOwners = poolOwnersHistoryInEpoch.getActiveStake();
                    poolRewardCalculationResult.setPoolOwnerStakeAddresses(poolOwnersHistoryInEpoch.getStakeAddresses());

                    if (isHigherOrEquals(totalActiveStakeOfOwners, poolPledge)) {
                        BigDecimal relativeStakeOfPoolOwner = divide(poolPledge, adaInCirculation);
                        BigDecimal relativePoolStake = divide(poolStake, adaInCirculation);

                        // Step 8: Calculate optimal pool reward
                        BigInteger optimalPoolReward =
                                PoolRewardCalculation.calculateOptimalPoolReward(
                                        stakePoolRewardsPot,
                                        optimalPoolCount,
                                        influenceParam,
                                        relativePoolStake,
                                        relativeStakeOfPoolOwner);
                        poolRewardCalculationResult.setOptimalPoolReward(optimalPoolReward);

                        // Step 9: Calculate pool reward as optimal pool reward * apparent pool performance
                        BigInteger poolReward = PoolRewardCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);
                        poolRewardCalculationResult.setPoolReward(poolReward);

                        // Step 10: Calculate pool operator reward
                        BigInteger poolOperatorReward = PoolRewardCalculation.calculateLeaderReward(poolReward, poolMargin, poolFixedCost,
                                divide(totalActiveStakeOfOwners, adaInCirculation), relativePoolStake);

                        // Step 10 a: Check if pool reward address has been unregistered before
                        List<String> stakeAddresses = new ArrayList<>();
                        stakeAddresses.add(poolRewardCalculationResult.getRewardAddress());
                        stakeAddresses.addAll(poolHistoryCurrentEpoch.getDelegators().stream().map(Delegator::getStakeAddress).toList());

                        List<AccountUpdate> accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(stakeAddresses, epoch - 1);
                        accountUpdates = accountUpdates.stream().filter(update ->
                                update.getAction().equals(AccountUpdateAction.DEREGISTRATION)
                                        || update.getAction().equals(AccountUpdateAction.REGISTRATION)).sorted(
                                Comparator.comparing(AccountUpdate::getUnixBlockTime).reversed()).toList();

                        Map<String, AccountUpdate> latestAccountUpdates = new HashMap<>();
                        for (AccountUpdate accountUpdate : accountUpdates) {
                            if (latestAccountUpdates.keySet().stream().noneMatch(stakeAddress -> stakeAddress.equals(accountUpdate.getStakeAddress()))) {
                                latestAccountUpdates.put(accountUpdate.getStakeAddress(), accountUpdate);
                            }
                        }

                        if (accountUpdates.size() > 0 &&
                                (latestAccountUpdates.get(poolRewardCalculationResult.getRewardAddress()) == null ||
                                        latestAccountUpdates.get(poolRewardCalculationResult.getRewardAddress()).getAction().equals(AccountUpdateAction.DEREGISTRATION))) {
                            poolOperatorReward = BigInteger.ZERO;
                        }

                        poolOperatorReward = add(poolOperatorReward, PoolRewardCalculation.correctOutliers(poolId, epoch));

                        poolRewardCalculationResult.setOperatorReward(poolOperatorReward);
                        totalDistributedRewards = add(totalDistributedRewards, poolOperatorReward);

                        // Step 11: Calculate pool member reward
                        List<Reward> memberRewards = new ArrayList<>();
                        for (Delegator delegator : poolHistoryCurrentEpoch.getDelegators()) {
                            if (accountUpdates.size() > 0 &&
                                    (latestAccountUpdates.get(delegator.getStakeAddress()) == null ||
                                            latestAccountUpdates.get(delegator.getStakeAddress()).getAction().equals(AccountUpdateAction.DEREGISTRATION))) {
                                continue;
                            }

                            if (delegator.getStakeAddress().equals(poolHistoryCurrentEpoch.getRewardAddress()) ||
                                    poolOwnersHistoryInEpoch.getStakeAddresses().contains(delegator.getStakeAddress())) {
                                continue;
                            }

                            BigInteger memberReward = PoolRewardCalculation.calculateMemberReward(poolReward, poolMargin,
                                    poolFixedCost, divide(delegator.getActiveStake(), adaInCirculation), relativePoolStake);
                            memberRewards.add(Reward.builder()
                                    .amount(memberReward)
                                    .stakeAddress(delegator.getStakeAddress())
                                    .build());
                        }
                        poolRewardCalculationResult.setMemberRewards(memberRewards);
                    } else {
                        System.out.println("Pool " + poolId + " has not enough pledge to be considered for rewards");
                    }
                }
            }

            System.out.println("Pool (" + i +  "/" + poolIds.size() + ") " + poolId + " reward: " + poolRewardCalculationResult.getPoolReward());

            for (Reward reward : actualPoolRewardsInEpoch) {
                List<Reward> memberRewards = poolRewardCalculationResult.getMemberRewards();
                if (memberRewards == null || memberRewards.isEmpty()) {
                    System.out.println("No member rewards found for pool " + poolId + " in epoch " + epoch);
                    System.out.println("But in db there is a reward of: " + reward.getAmount() + " ADA for " + reward.getStakeAddress());
                    continue;
                }

                Reward memberReward = memberRewards.stream()
                        .filter(member -> member.getStakeAddress().equals(reward.getStakeAddress()))
                        .findFirst()
                        .orElse(null);

                if (memberReward == null) {
                    System.out.println("Member " + reward.getStakeAddress() + " reward of " + reward.getAmount() + " not found in the calculated rewards");
                    continue;
                }

                totalDistributedRewards = add(totalDistributedRewards, reward.getAmount());
            }

            PoolRewardCalculationResults.add(poolRewardCalculationResult);
        }

        BigInteger calculatedReserve = subtract(reserveInPreviousEpoch, subtract(rewardPot, totalFeesForCurrentEpoch));
        BigInteger undistributedRewards = subtract(stakePoolRewardsPot, totalDistributedRewards);
        calculatedReserve = add(calculatedReserve, undistributedRewards);

        epochCalculationResult.setTotalDistributedRewards(totalDistributedRewards);
        epochCalculationResult.setTotalRewardsPot(rewardPot);
        epochCalculationResult.setReserves(calculatedReserve);
        epochCalculationResult.setTreasury(treasuryForCurrentEpoch);
        epochCalculationResult.setPoolRewardCalculationResults(PoolRewardCalculationResults);
        epochCalculationResult.setTotalPoolRewardsPot(stakePoolRewardsPot);
        epochCalculationResult.setTotalAdaInCirculation(adaInCirculation);
        epochCalculationResult.setTotalUndistributedRewards(undistributedRewards);

        return epochCalculationResult;
    }
}
