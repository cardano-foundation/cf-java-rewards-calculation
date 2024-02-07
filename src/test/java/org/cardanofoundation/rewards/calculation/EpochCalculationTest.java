package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.util.List;

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

    public void testCalculateEpochRewards(final int epoch) {
        List<String> poolIds = dataProvider.getPoolsThatProducedBlocksInEpoch(epoch);
        double totalDifference = 0.0;
        double totalDistributedRewards = 0.0;

        AdaPots adaPotsForNextEpoch = dataProvider.getAdaPotsForEpoch(epoch + 1);
        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch);
        Epoch epochInfo = dataProvider.getEpochInfo(epoch);

        for (int i = 0; i < poolIds.size(); i++) {
            String poolId = poolIds.get(i);

            long start = System.currentTimeMillis();
            List<Reward> actualPoolRewardsInEpoch = dataProvider.getRewardListForPoolInEpoch(epoch, poolId);
            System.out.println("Pool rewards for pool " + poolId + " in epoch " + epoch + " fetched in " + (System.currentTimeMillis() - start) + " ms");

            start = System.currentTimeMillis();
            PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculation.calculatePoolRewardInEpoch(poolId, epochInfo, adaPotsForNextEpoch, protocolParameters, dataProvider);
            System.out.println("Pool reward for pool " + poolId + " in epoch " + epoch + " calculated in " + (System.currentTimeMillis() - start) + " ms");

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
                    totalDistributedRewards += poolOwnerReward;
                }

                if (difference > 0.0) {
                    System.out.println("The difference between expected member " + reward.getStakeAddress() + " reward and actual member reward is : " + lovelaceToAda(difference) + " ADA");
                }

                totalDifference += difference;
                totalDistributedRewards += reward.getAmount();
            }

            System.out.println("Total difference: " + lovelaceToAda(totalDifference) + " ADA");
        }

        System.out.println("Total difference for epoch " + epoch + ": " + lovelaceToAda(totalDifference) + " ADA");
        System.out.println("Total distributed rewards for epoch " + epoch + ": " + lovelaceToAda(totalDistributedRewards) + " ADA");
        System.out.println("Total available rewards for epoch " + epoch + ": " + lovelaceToAda(adaPotsForNextEpoch.getRewards()));
    }

    @Test
    public void testCalculateEpochRewardsForEpoch215() {
        testCalculateEpochRewards(215);
    }
}
