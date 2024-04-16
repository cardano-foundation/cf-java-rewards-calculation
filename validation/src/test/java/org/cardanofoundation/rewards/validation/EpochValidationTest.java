package org.cardanofoundation.rewards.validation;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.AdaPots;
import org.cardanofoundation.rewards.calculation.domain.EpochCalculationResult;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
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

@SpringBootTest
@ComponentScan
@Slf4j
public class EpochValidationTest {
    @Autowired(required = false)
    DbSyncDataProvider dbSyncDataProvider;

    @Autowired
    JsonDataProvider jsonDataProvider;

    public void testCalculateEpochPots(final int epoch, DataProvider dataProvider, boolean detailedValidation) {
        EpochCalculationResult epochCalculationResult = EpochValidation.calculateEpochRewardPots(epoch, dataProvider, detailedValidation);
        AdaPots adaPotsForCurrentEpoch = dataProvider.getAdaPotsForEpoch(epoch);

        log.info("Treasury difference: " + adaPotsForCurrentEpoch.getTreasury().subtract(epochCalculationResult.getTreasury()).longValue() + " Lovelace");
        log.info("Reserves difference: " + adaPotsForCurrentEpoch.getReserves().subtract(epochCalculationResult.getReserves()).longValue() + " Lovelace");

        Assertions.assertEquals(epoch, epochCalculationResult.getEpoch());
        Assertions.assertEquals(adaPotsForCurrentEpoch.getTreasury(), epochCalculationResult.getTreasury());
        Assertions.assertEquals(adaPotsForCurrentEpoch.getReserves(), epochCalculationResult.getReserves());
    }

    static Stream<Integer> dataProviderEpochRange() {
        return IntStream.range(208, 230).boxed();
    }

    @ParameterizedTest
    @MethodSource("dataProviderEpochRange")
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    public void testCalculateEpochRewardsWithDbSyncDataProvider(int epoch) {
        testCalculateEpochPots(epoch, dbSyncDataProvider, false);
    }

    @ParameterizedTest
    @MethodSource("dataProviderEpochRange")
    public void testCalculateEpochRewardsWithJsonDataProvider(int epoch) {
        testCalculateEpochPots(epoch, jsonDataProvider, false);
    }

    static Stream<Integer> testEpochs() {
        return Stream.of(
                360, 475
        );
    }

    @ParameterizedTest
    @MethodSource("testEpochs")
    public void testYoungerEpochRewardsWithJsonDataProvider(int epoch) {
        testCalculateEpochPots(epoch, jsonDataProvider, false);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    public void testCalculateEpochRewardsForEpoch417() {
        testCalculateEpochPots(417, dbSyncDataProvider, true);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    public void testCalculateEpochRewardsForEpoch367() {
        testCalculateEpochPots(365, jsonDataProvider, false);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    public void testCalculateEpochRewardsForEpoch350() {
        testCalculateEpochPots(350, dbSyncDataProvider, true);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    public void testCalculateEpochRewardsForEpoch425() {
        testCalculateEpochPots(425, dbSyncDataProvider, false);
    }
}
