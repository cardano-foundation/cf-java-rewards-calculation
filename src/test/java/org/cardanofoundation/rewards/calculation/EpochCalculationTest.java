package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;
import org.cardanofoundation.rewards.enums.MirPot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.util.*;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

/*
 * This class is used to test the calculation of all ada pots for a given epoch.
 */
@SpringBootTest
@ComponentScan
@EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
public class EpochCalculationTest {

    // Only the DbSyncDataProvider is used for this test
    // as the amount of data would be too much for the Koios or JSON data provider .
    @Autowired
    DbSyncDataProvider dataProvider;

    public void testCalculateEpochPots(final int epoch) {
        double treasuryGrowthRate = 0.2;
        double monetaryExpandRate = 0.003;
        double decentralizationParameter = 1;

        AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
        AdaPots adaPotsForCurrentEpoch = dataProvider.getAdaPotsForEpoch(epoch);

        double totalFeesForCurrentEpoch = 0.0;
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

        double reserveInPreviousEpoch = adaPotsForPreviousEpoch.getReserves();

        double treasuryInPreviousEpoch = adaPotsForPreviousEpoch.getTreasury();
        double expectedTreasuryForCurrentEpoch = adaPotsForCurrentEpoch.getTreasury();

        double rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

        double treasuryCut = Math.floor(rewardPot * treasuryGrowthRate);
        double treasuryForCurrentEpoch = treasuryInPreviousEpoch + treasuryCut;
        double stakePoolRewardsPot = rewardPot - treasuryCut;

        // The sum of all the refunds attached to unregistered reward accounts are added to the
        // treasury (see: Pool Reap Transition, p.53, figure 40, shely-ledger.pdf)
        treasuryForCurrentEpoch += TreasuryCalculation.calculateUnclaimedRefundsForRetiredPools(epoch, dataProvider);

        // Check if there was a MIR Certificate in the previous epoch
        double treasuryWithdrawals = 0.0;
        List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
        for (MirCertificate mirCertificate : mirCertificates) {
            if (mirCertificate.getPot() == MirPot.TREASURY) {
                treasuryWithdrawals += mirCertificate.getTotalRewards();
            }
        }
        treasuryForCurrentEpoch -= treasuryWithdrawals;

        List<String> poolIds = dataProvider.getPoolsThatProducedBlocksInEpoch(epoch - 2);
        double totalDifference = 0.0;
        double totalDistributedRewards = 0.0;

        double adaInCirculation = TOTAL_LOVELACE - reserveInPreviousEpoch;
        int optimalPoolCount = protocolParameters.getOptimalPoolCount();
        double influenceParam = protocolParameters.getPoolOwnerInfluence();

        for (int i = 0; i < poolIds.size(); i++) {
            String poolId = poolIds.get(i);

            List<Reward> actualPoolRewardsInEpoch = dataProvider.getRewardListForPoolInEpoch(epoch - 2, poolId);
            PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculationResult.builder()
                    .epoch(epoch)
                    .poolId(poolId)
                    .poolReward(0.0)
                    .build();

            PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch - 2);
            if(poolHistoryCurrentEpoch != null) {
                double poolStake = poolHistoryCurrentEpoch.getActiveStake();
                double poolFees = poolHistoryCurrentEpoch.getPoolFees();
                double poolMargin = poolHistoryCurrentEpoch.getMargin();
                double poolFixedCost = poolHistoryCurrentEpoch.getFixedCost();
                int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCount();

                poolRewardCalculationResult.setPoolFee(poolFees);
                poolRewardCalculationResult.setPoolMargin(poolMargin);
                poolRewardCalculationResult.setPoolCost(poolFixedCost);
                poolRewardCalculationResult.setRewardAddress(poolHistoryCurrentEpoch.getRewardAddress());

                if (blocksPoolHasMinted > 0) {
                    double activeStakeInEpoch = 0;
                    if (epochInfo.getActiveStake() != null) {
                        activeStakeInEpoch = epochInfo.getActiveStake();
                    }

                    // Step 5: Calculate apparent pool performance
                    double apparentPoolPerformance =
                            PoolRewardCalculation.calculateApparentPoolPerformance(poolStake, activeStakeInEpoch,
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
                    double totalActiveStakeOfOwners = poolOwnersHistoryInEpoch.getActiveStake();
                    poolRewardCalculationResult.setPoolOwnerStakeAddresses(poolOwnersHistoryInEpoch.getStakeAddresses());

                    if (totalActiveStakeOfOwners >= poolPledge) {
                        double relativeStakeOfPoolOwner = poolPledge / adaInCirculation;
                        double relativePoolStake = poolStake / adaInCirculation;

                        // Step 8: Calculate optimal pool reward
                        double optimalPoolReward =
                                PoolRewardCalculation.calculateOptimalPoolReward(
                                        stakePoolRewardsPot,
                                        optimalPoolCount,
                                        influenceParam,
                                        relativePoolStake,
                                        relativeStakeOfPoolOwner);
                        poolRewardCalculationResult.setOptimalPoolReward(optimalPoolReward);

                        // Step 9: Calculate pool reward as optimal pool reward * apparent pool performance
                        double poolReward = PoolRewardCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);
                        poolRewardCalculationResult.setPoolReward(poolReward);

                        // Step 10: Calculate pool operator reward
                        double poolOperatorReward = PoolRewardCalculation.calculateLeaderReward(poolReward, poolMargin, poolFixedCost,
                                totalActiveStakeOfOwners / adaInCirculation, relativePoolStake);

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
                            poolOperatorReward = 0.0;
                        }

                        poolOperatorReward += PoolRewardCalculation.correctOutliers(poolId, epoch);

                        poolRewardCalculationResult.setOperatorReward(poolOperatorReward);
                        totalDistributedRewards += poolOperatorReward;

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

                            double memberReward = PoolRewardCalculation.calculateMemberReward(poolReward, poolMargin,
                                    poolFixedCost, delegator.getActiveStake() / adaInCirculation, relativePoolStake);
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

                double difference = Math.abs(reward.getAmount() - memberReward.getAmount());

                if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(reward.getStakeAddress())) {
                    double poolOwnerReward = poolRewardCalculationResult.getOperatorReward();
                    difference = Math.abs(reward.getAmount() - poolOwnerReward);
                }

                if (difference > 0.0) {
                    System.out.println("The difference between expected member " + reward.getStakeAddress() + " reward and actual member reward is : " + lovelaceToAda(difference) + " ADA");
                }

                totalDifference += difference;
                totalDistributedRewards += reward.getAmount();
            }

            Double actualTotalPoolRewardsInEpoch = dataProvider.getTotalPoolRewardsInEpoch(poolId, epoch - 2);

            if (actualTotalPoolRewardsInEpoch == null) {
                actualTotalPoolRewardsInEpoch = 0.0;
            }

            double calculatedTotalPoolRewardsInEpoch = 0.0;

            if (poolRewardCalculationResult.getMemberRewards() != null) {
                calculatedTotalPoolRewardsInEpoch = poolRewardCalculationResult.getMemberRewards().stream()
                        .map(Reward::getAmount)
                        .mapToDouble(Double::doubleValue).sum() + poolRewardCalculationResult.getOperatorReward();
            }

            Assertions.assertEquals(actualTotalPoolRewardsInEpoch, calculatedTotalPoolRewardsInEpoch,
                    "The difference between expected pool reward and actual pool reward is : " + lovelaceToAda(actualTotalPoolRewardsInEpoch - calculatedTotalPoolRewardsInEpoch) + " ADA");

            System.out.println("Total difference: " + lovelaceToAda(totalDifference) + " ADA");
        }

        System.out.println("Total reward difference over all pools for epoch " + epoch + ": " + lovelaceToAda(totalDifference) + " ADA");
        System.out.println("Total distributed rewards for epoch " + epoch + ": " + lovelaceToAda(totalDistributedRewards) + " ADA");
        System.out.println("Expected total rewards for epoch " + epoch + ": " + lovelaceToAda(adaPotsForCurrentEpoch.getRewards()) + " ADA");
        System.out.println("Total fees for epoch " + epoch + ": " + lovelaceToAda(totalFeesForCurrentEpoch) + " ADA");
        System.out.println("Calculated total reward pot for epoch " + epoch + ": " + lovelaceToAda(rewardPot) + " ADA");
        System.out.println("Calculated stake pool rewards pot for epoch " + epoch + ": " + lovelaceToAda(stakePoolRewardsPot) + " ADA");
        double calculatedReserveForCurrentEpoch = reserveInPreviousEpoch - (rewardPot - totalFeesForCurrentEpoch);
        double undistributedRewards = stakePoolRewardsPot - totalDistributedRewards;
        calculatedReserveForCurrentEpoch += undistributedRewards;
        System.out.println("Undistributed rewards for epoch " + epoch + ": " + lovelaceToAda(undistributedRewards) + " ADA");
        System.out.println("Calculated reserve for epoch " + epoch + ": " + lovelaceToAda(calculatedReserveForCurrentEpoch) + " ADA");
        System.out.println("Expected reserve for epoch " + epoch + ": " + lovelaceToAda(adaPotsForCurrentEpoch.getReserves()) + " ADA");
        System.out.println("Treasury for next epoch: " + lovelaceToAda(treasuryForCurrentEpoch) + " ADA");
        System.out.println("Expected treasury for epoch " + epoch + ": " + lovelaceToAda(expectedTreasuryForCurrentEpoch) + " ADA");
    }

    @Test
    public void testCalculateEpochRewardsForEpoch213() {
        testCalculateEpochPots(213);
    }

    @Test
    public void testCalculateEpochRewardsForEpoch214() {
        testCalculateEpochPots(214);
    }
}
