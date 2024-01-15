package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.enums.DataProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.IntStream;
import java.util.stream.Stream;

@SpringBootTest
@ComponentScan
@ActiveProfiles("db-sync")
public class UtxoCalculation {

    @Autowired
    KoiosDataProvider koiosDataProvider;

    @Autowired
    JsonDataProvider jsonDataProvider;

    @Autowired
    DbSyncDataProvider dbSyncDataProvider;

    void Test_calculateUtxoPot(final int epoch, DataProviderType dataProviderType) {
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

        AdaPots adaPots = dataProvider.getAdaPotsForEpoch(epoch);
        double utxo = 31111977147073356.0;

        if (epoch > 208) {
            double utxoFromPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1).getAdaInCirculation();
            double deposits = dataProvider.getTransactionDepositsInEpoch(epoch - 1);
            double fees = dataProvider.getSumOfFeesInEpoch(epoch - 1);
            double withdrawals = 0.0;

            if (epoch > 209) {
                withdrawals = dataProvider.getSumOfWithdrawalsInEpoch(epoch - 1);
            }

            utxo = utxoFromPreviousEpoch - deposits - fees + withdrawals;
        }

        double difference = adaPots.getAdaInCirculation() - utxo;
        Assertions.assertEquals(0.0, difference);
    }

    static Stream<Integer> dataProviderRangeUntilEpoch213() {
        return IntStream.range(208, 460).boxed();
    }

    @ParameterizedTest
    @MethodSource("dataProviderRangeUntilEpoch213")
    void Test_calculateUtxoPotWithDbSyncDataProvider(int epoch) {
        Test_calculateUtxoPot(epoch, DataProviderType.DB_SYNC);
    }

    @Test
    void Test_calculateUtxoPotWithDbSyncDataProviderForEpoch236() {
        Test_calculateUtxoPot(236, DataProviderType.DB_SYNC);
    }
}
