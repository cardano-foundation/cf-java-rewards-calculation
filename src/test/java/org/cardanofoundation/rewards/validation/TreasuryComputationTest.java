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
public class TreasuryComputationTest {

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

    TreasuryCalculationResult treasuryCalculationResult = TreasuryComputation.calculateTreasuryForEpoch(epoch, dataProvider);
    AdaPots adaPots = dataProvider.getAdaPotsForEpoch(epoch);

    BigInteger difference = adaPots.getTreasury().subtract(treasuryCalculationResult.getTreasury());
    Assertions.assertEquals(BigInteger.ZERO, difference, "The difference " + lovelaceToAda(difference.intValue()) + " ADA between expected treasury value and actual treasury value is greater than 1 LOVELACE");
  }

  static Stream<Integer> dataProviderRange() {
    return IntStream.range(208, 215).boxed();
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


  private static Stream<Arguments> retiredPoolTestRange() {
    return Stream.of(
            Arguments.of(210, BigInteger.ZERO),
            Arguments.of(211, POOL_DEPOSIT_IN_LOVELACE),
            Arguments.of(212, BigInteger.ZERO),
            Arguments.of(213, POOL_DEPOSIT_IN_LOVELACE),
            Arguments.of(214, POOL_DEPOSIT_IN_LOVELACE),
            Arguments.of(215, POOL_DEPOSIT_IN_LOVELACE),
            Arguments.of(216, BigInteger.ZERO),
            Arguments.of(219, BigInteger.ZERO),
            Arguments.of(222, BigInteger.ZERO)
    );
  }
  @ParameterizedTest
  @MethodSource("retiredPoolTestRange")
  void Test_calculateUnclaimedRefundsForRetiredPools(int epoch, BigInteger expectedUnclaimedRefunds) {
    List<PoolDeregistration> retiredPools = jsonDataProvider.getRetiredPoolsInEpoch(epoch);
    List<AccountUpdate> accountUpdates = jsonDataProvider.getAccountUpdatesUntilEpoch(
            retiredPools.stream().map(PoolDeregistration::getRewardAddress).toList(), epoch - 1);
    BigInteger unclaimedRefunds = TreasuryComputation.calculateUnclaimedRefundsForRetiredPools(retiredPools, accountUpdates);
    Assertions.assertEquals(expectedUnclaimedRefunds, unclaimedRefunds);
  }

  @ParameterizedTest
  @MethodSource("retiredPoolTestRange")
  @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
  void Test_calculateUnclaimedRefundsForRetiredPoolsWithDbSync(int epoch, BigInteger expectedUnclaimedRefunds) {
    List<PoolDeregistration> retiredPools = dbSyncDataProvider.getRetiredPoolsInEpoch(epoch);
    List<AccountUpdate> accountUpdates = dbSyncDataProvider.getAccountUpdatesUntilEpoch(
            retiredPools.stream().map(PoolDeregistration::getRewardAddress).toList(), epoch - 1);
    BigInteger unclaimedRefunds = TreasuryComputation.calculateUnclaimedRefundsForRetiredPools(retiredPools, accountUpdates);
    Assertions.assertEquals(expectedUnclaimedRefunds, unclaimedRefunds);
  }
}
