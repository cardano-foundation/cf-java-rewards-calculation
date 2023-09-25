package org.cardanofoundation.rewards.calculation;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.cardanofoundation.rewards.entity.TreasuryCalculationResult;
import org.cardanofoundation.rewards.enums.DataProviderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.ComponentScan;
import static org.cardanofoundation.rewards.constants.RewardConstants.*;
import static org.cardanofoundation.rewards.util.CurrencyConverter.adaToLovelace;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class TreasuryCalculationTest {

  @Autowired
  KoiosDataProvider koiosDataProvider;

  @Autowired
  JsonDataProvider jsonDataProvider;

  void Test_calculateTreasury(final int epoch, DataProviderType dataProviderType) {

    DataProvider dataProvider = null;
    if (dataProviderType == DataProviderType.KOIOS) {
      dataProvider = koiosDataProvider;
    } else if (dataProviderType == DataProviderType.JSON) {
      dataProvider = jsonDataProvider;
    } else {
      throw new RuntimeException("Unknown data provider type: " + dataProviderType);
    }

    TreasuryCalculationResult treasuryCalculationResult = TreasuryCalculation.calculateTreasuryForEpoch(epoch, dataProvider);

    double difference = treasuryCalculationResult.getActualTreasury() - treasuryCalculationResult.getCalculatedTreasury();
    Assertions.assertTrue(Math.abs(difference) < 1, "The difference between expected pool reward and actual pool reward is greater than 1 LOVELACE: " + lovelaceToAda(difference) + " ADA");
  }

  static Stream<Integer> koiosDataProviderRange() {
    return IntStream.range(210, 213).boxed();
  }

  @ParameterizedTest
  @MethodSource("koiosDataProviderRange")
  void Test_calculateTreasuryWithKoiosDataProvider(int epoch) {
    Test_calculateTreasury(epoch, DataProviderType.KOIOS);
  }

  static Stream<Integer> jsonDataProviderRange() {
    return IntStream.range(210, 430).boxed();
  }

  @ParameterizedTest
  @MethodSource("jsonDataProviderRange")
  void Test_calculateTreasuryWithJsonDataProvider(int epoch) {
    Test_calculateTreasury(epoch, DataProviderType.JSON);
  }

  private static Stream<Arguments> retiredPoolTestRange() {
    return Stream.of(
            Arguments.of(210, 0.0),
            Arguments.of(211, DEPOSIT_POOL_REGISTRATION_IN_LOVELACE),
            Arguments.of(212, 0.0),
            Arguments.of(213, DEPOSIT_POOL_REGISTRATION_IN_LOVELACE),
            Arguments.of(214, DEPOSIT_POOL_REGISTRATION_IN_LOVELACE),
            Arguments.of(215, DEPOSIT_POOL_REGISTRATION_IN_LOVELACE),
            Arguments.of(216, 0.0),
            Arguments.of(219, 0.0),
            Arguments.of(222, 0.0)
    );
  }
  @ParameterizedTest
  @MethodSource("retiredPoolTestRange")
  void Test_calculateUnclaimedRefundsForRetiredPools(int epoch, double expectedUnclaimedRefunds) {
    double unclaimedRefunds = TreasuryCalculation.calculateUnclaimedRefundsForRetiredPools(epoch, jsonDataProvider);
    Assertions.assertEquals(expectedUnclaimedRefunds, unclaimedRefunds);
  }
}
