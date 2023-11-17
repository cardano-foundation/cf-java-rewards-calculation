package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.entity.PoolRewardCalculationResult;
import org.cardanofoundation.rewards.entity.TreasuryCalculationResult;
import org.cardanofoundation.rewards.enums.DataProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

/*
 * This class is used to test the calculation of all ada pots for a given epoch.
 */
@SpringBootTest
@ComponentScan
@ActiveProfiles("db-sync")
public class EpochCalculationTest {

    // Only the DbSyncDataProvider is used for this test
    // as the amount of data would be too much for the Koios or JSON data provider .
    @Autowired
    DbSyncDataProvider dataProvider;

    public void testCalculateEpochRewards(final int epoch) {
        TreasuryCalculationResult treasuryCalculationResult = TreasuryCalculation.calculateTreasuryForEpoch(epoch, dataProvider);
        List<String> poolIds = dataProvider.getPoolsThatProducedBlocksInEpoch(epoch);

        for (int i = 0; i < poolIds.size(); i++) {
            String poolId = poolIds.get(i);
            PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculation.calculatePoolRewardInEpoch(poolId, epoch, dataProvider);
            System.out.println("Pool (" + i +  "/" + poolIds.size() + ") " + poolId + " reward: " + poolRewardCalculationResult.getPoolReward());
        }
    }

    @Test
    public void testCalculateEpochRewardsForEpoch215() {
        testCalculateEpochRewards(215);
    }
}
