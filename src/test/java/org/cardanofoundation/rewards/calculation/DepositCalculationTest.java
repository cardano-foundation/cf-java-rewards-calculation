package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.PoolDeregistration;
import org.cardanofoundation.rewards.entity.PoolUpdate;
import org.cardanofoundation.rewards.enums.DataProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.cardanofoundation.rewards.constants.RewardConstants.DEPOSIT_POOL_REGISTRATION_IN_LOVELACE;

@SpringBootTest
@ComponentScan
@ActiveProfiles("db-sync")
public class DepositCalculationTest {

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
        double actualDepositsInNextEpoch = adaPotsForNextEpoch.getDeposits();
        double calculatedDepositsInNextEpoch = DepositCalculation.calculateDepositInEpoch(epoch, dataProvider);

        double difference = actualDepositsInNextEpoch - calculatedDepositsInNextEpoch;
        Assertions.assertEquals(0.0, difference);
    }

    static Stream<Integer> dataProviderRangeUntilEpoch213() {
        return IntStream.range(208, 460).boxed();
    }

    @ParameterizedTest
    @MethodSource("dataProviderRangeUntilEpoch213")
    void Test_calculateDepositsWithDbSyncDataProvider(int epoch) {
        Test_calculateDeposit(epoch, DataProviderType.DB_SYNC);
    }

    @Test
    void Test_calculateDepositsWithDbSyncDataProviderInEpoch246() {
        Test_calculateDeposit(246, DataProviderType.DB_SYNC);
    }

    @Test
    void Test_countPoolRegistrationsInEpoch211() {
        Assertions.assertEquals(54, dbSyncDataProvider.getPoolRegistrationsInEpoch(211));
    }

}
