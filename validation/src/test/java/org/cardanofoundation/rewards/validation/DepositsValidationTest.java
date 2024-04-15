package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.calculation.domain.AdaPots;
import org.cardanofoundation.rewards.validation.enums.DataProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootTest
@ComponentScan
@EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
public class DepositsValidationTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;

    @Autowired
    JsonDataProvider jsonDataProvider;

    @Autowired
    DbSyncDataProvider dbSyncDataProvider;

    void Test_calculateDeposit(final int epoch, DataProviderType dataProviderType) {

        final DataProvider dataProvider;
        if (dataProviderType == DataProviderType.KOIOS) {
            dataProvider = koiosDataProvider;
        } else if (dataProviderType == DataProviderType.JSON) {
            dataProvider = jsonDataProvider;
        } else if (dataProviderType == DataProviderType.DB_SYNC) {
            dataProvider = dbSyncDataProvider;
        } else {
            throw new RuntimeException("Unknown data provider type: " + dataProviderType);
        }

        AdaPots adaPotsForNextEpoch = dataProvider.getAdaPotsForEpoch(epoch + 1);
        BigInteger actualDepositsInNextEpoch = adaPotsForNextEpoch.getDeposits();
        BigInteger calculatedDepositsInNextEpoch = DepositsValidation.computeDepositsInEpoch(epoch, dataProvider);

        BigInteger difference = actualDepositsInNextEpoch.subtract(calculatedDepositsInNextEpoch);
        Assertions.assertEquals(BigInteger.ZERO, difference);
    }

    static Stream<Integer> dataProviderRangeUntilEpoch460() {
        return IntStream.range(208, 460).boxed();
    }

    @ParameterizedTest
    @MethodSource("dataProviderRangeUntilEpoch460")
    void Test_calculateDepositsWithDbSyncDataProvider(int epoch) {
        Test_calculateDeposit(epoch, DataProviderType.DB_SYNC);
    }

    @Test
    void Test_calculateDepositsWithDbSyncDataProviderInEpoch246() {
        Test_calculateDeposit(246, DataProviderType.DB_SYNC);
    }

}
