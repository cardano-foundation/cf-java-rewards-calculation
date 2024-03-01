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
public class FeesValidationTest {
    @Autowired
    KoiosDataProvider koiosDataProvider;

    @Autowired
    JsonDataProvider jsonDataProvider;

    @Autowired
    DbSyncDataProvider dbSyncDataProvider;

    void Test_calculateFees(final int epoch, DataProviderType dataProviderType) {
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
        BigInteger fees = FeeValidation.calculateFeePotInEpoch(epoch, dataProvider);

        BigInteger difference = adaPots.getFees().subtract(fees);
        Assertions.assertEquals(BigInteger.ZERO, difference);
    }

    static Stream<Integer> dataProviderRangeUntilEpoch213() {
        return IntStream.range(208, 460).boxed();
    }

    @ParameterizedTest
    @MethodSource("dataProviderRangeUntilEpoch213")
    void Test_calculateFeesWithDbSyncDataProvider(int epoch) {
        Test_calculateFees(epoch, DataProviderType.DB_SYNC);
    }

    @Test
    void Test_calculateFeesWithDbSyncDataProviderForEpoch214() {
        Test_calculateFees(214, DataProviderType.DB_SYNC);
    }
}
