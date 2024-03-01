package org.cardanofoundation.rewards.validation;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.calculation.domain.AccountUpdate;
import org.cardanofoundation.rewards.calculation.domain.AdaPots;
import org.cardanofoundation.rewards.calculation.domain.PoolDeregistration;
import org.cardanofoundation.rewards.calculation.domain.TreasuryCalculationResult;
import org.cardanofoundation.rewards.validation.domain.TreasuryValidationResult;
import org.cardanofoundation.rewards.validation.enums.DataProviderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;
import static org.cardanofoundation.rewards.calculation.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class TreasuryValidationTest {

  @Autowired
  KoiosDataProvider koiosDataProvider;

  @Autowired
  JsonDataProvider jsonDataProvider;

  @Autowired(required = false)
  DbSyncDataProvider dbSyncDataProvider;

  void Test_calculateTreasury(final int epoch, DataProviderType dataProviderType) {

    DataProvider dataProvider;
    if (dataProviderType == DataProviderType.KOIOS) {
      dataProvider = koiosDataProvider;
    } else if (dataProviderType == DataProviderType.JSON) {
      dataProvider = jsonDataProvider;
    } else if (dataProviderType == DataProviderType.DB_SYNC) {
      dataProvider = dbSyncDataProvider;
    } else {
      throw new RuntimeException("Unknown data provider type: " + dataProviderType);
    }

    TreasuryValidationResult treasuryValidationResult = TreasuryValidation.calculateTreasuryForEpoch(epoch, dataProvider);
    BigInteger difference = treasuryValidationResult.getActualTreasury().subtract(treasuryValidationResult.getCalculatedTreasury());
    Assertions.assertEquals(BigInteger.ZERO, difference, "The difference " + lovelaceToAda(difference.intValue()) + " ADA between expected treasury value and actual treasury value is not zero");
  }

  static Stream<Integer> dataProviderRange() {
    return IntStream.range(209, 215).boxed();
  }

  @ParameterizedTest
  @MethodSource("dataProviderRange")
  void Test_calculateTreasuryWithJsonDataProvider(int epoch) {
    Test_calculateTreasury(epoch, DataProviderType.JSON);
  }

  @ParameterizedTest
  @MethodSource("dataProviderRange")
  @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
  void Test_calculateTreasuryWithDbSyncDataProvider(int epoch) {
    Test_calculateTreasury(epoch, DataProviderType.DB_SYNC);
  }
}
