package org.cardanofoundation.rewards.calculation;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.cardanofoundation.rewards.enums.DataProviderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.ComponentScan;
import static org.cardanofoundation.rewards.constants.RewardConstants.*;
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

    ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
    final double treasuryGrowthRate = protocolParameters.getTreasuryGrowRate();
    final double monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
    double decentralizationParameter = protocolParameters.getDecentralisation();

    AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
    AdaPots adaPotsForCurrentEpoch = dataProvider.getAdaPotsForEpoch(epoch);

    // The Shelley era and the ada pot system started on mainnet in epoch 208.
    // Fee and treasury values are 0 for epoch 208.
    double totalFeesForCurrentEpoch = 0.0;
    Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);
    if (epoch > 209) {
      totalFeesForCurrentEpoch = epochInfo.getFees();
    }

    double reserveInPreviousEpoch = adaPotsForPreviousEpoch.getReserves();

    double treasuryInPreviousEpoch = adaPotsForPreviousEpoch.getTreasury();
    double expectedTreasuryForCurrentEpoch = adaPotsForCurrentEpoch.getTreasury();

    int totalBlocksInEpoch = epochInfo.getBlockCount();

    if (epoch > 214 && epoch < 257) {
        totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
    }

    double rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
            monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

    double treasuryForCurrentEpoch = TreasuryCalculation.calculateTreasury(
            treasuryGrowthRate, rewardPot, treasuryInPreviousEpoch);


    // The sum of all the refunds attached to unregistered reward accounts are added to the
    // treasury (see: Pool Reap Transition, p.53, figure 40, shely-ledger.pdf)
    treasuryForCurrentEpoch += TreasuryCalculation.calculateUnclaimedRefundsForRetiredPools(epoch, dataProvider);

    long differenceInADA = Math.round(lovelaceToAda(expectedTreasuryForCurrentEpoch - treasuryForCurrentEpoch));

    System.out.println("Difference in ADA: " + differenceInADA);

    Assertions.assertEquals(Math.floor(expectedTreasuryForCurrentEpoch), Math.floor(treasuryForCurrentEpoch));
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
    return IntStream.range(210, 215).boxed();
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
