package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        EpochCalculationResult epochCalculationResult = EpochCalculation.calculateEpochPots(epoch, dataProvider);
        AdaPots adaPotsForCurrentEpoch = dataProvider.getAdaPotsForEpoch(epoch);

        Assertions.assertEquals(epoch, epochCalculationResult.getEpoch());
        Assertions.assertEquals(adaPotsForCurrentEpoch.getRewards(), epochCalculationResult.getTotalDistributedRewards());
        Assertions.assertEquals(adaPotsForCurrentEpoch.getTreasury(), epochCalculationResult.getTreasury());
        Assertions.assertEquals(adaPotsForCurrentEpoch.getReserves(), epochCalculationResult.getReserves());
        Assertions.assertEquals(adaPotsForCurrentEpoch.getAdaInCirculation(), epochCalculationResult.getTotalAdaInCirculation());
    }

    static Stream<Integer> dataProviderEpochRange() {
        return IntStream.range(213, 217).boxed();
    }

    @ParameterizedTest
    @MethodSource("dataProviderEpochRange")
    public void testCalculateEpochRewardsForEpoch213(int epoch) {
        testCalculateEpochPots(epoch);
    }

    @Test
    public void testCalculateEpochRewardsForEpoch215() {
        testCalculateEpochPots(215);
    }
}
