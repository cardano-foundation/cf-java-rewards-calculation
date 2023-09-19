package org.cardanofoundation.rewards.calculation;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.PoolUpdate;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.cardanofoundation.rewards.enums.DataProviderType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
        Epoch currentEpochInfo = dataProvider.getEpochInfo(epoch - 2);
        totalBlocksInEpoch = currentEpochInfo.getNonOBFTBlockCount();
    }

    double rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
            monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

    double treasuryForCurrentEpoch = TreasuryCalculation.calculateTreasury(
            treasuryGrowthRate, rewardPot, treasuryInPreviousEpoch);

    /*
      TODO: "For each retiring pool, the refund for the pool registration deposit is added to the
       pool's registered reward account, provided the reward account is still registered." -
       https://github.com/input-output-hk/cardano-ledger/blob/9e2f8151e3b9a0dde9faeb29a7dd2456e854427c/eras/shelley/formal-spec/epoch.tex#L546C9-L547C87


       int retiredPoolsWithDeregisteredRewardAddress = koiosDataProvider.countRetiredPoolsWithDeregisteredRewardAddress(epoch - 1);

       BigDecimal deposit = new BigDecimal(DEPOSIT_POOL_REGISTRATION_IN_ADA);
       treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(deposit.multiply(new BigDecimal(retiredPoolsWithDeregisteredRewardAddress)));
    */

    long differenceInADA = Math.round(lovelaceToAda(expectedTreasuryForCurrentEpoch - treasuryForCurrentEpoch));

    System.out.println("Difference in ADA: " + differenceInADA);

    if (differenceInADA > 0 && differenceInADA % DEPOSIT_POOL_REGISTRATION_IN_ADA == 0) {
      long numberOfPools = differenceInADA / DEPOSIT_POOL_REGISTRATION_IN_ADA;
      System.out.println("Probably there was/were " + numberOfPools + " retired pool(s) with deregistered reward address in epoch " + epoch +
              ". That's why " + differenceInADA + " ADA was added to the treasury.");
      Assertions.assertEquals(0, differenceInADA % DEPOSIT_POOL_REGISTRATION_IN_ADA);
    } else {
      Assertions.assertEquals(Math.floor(expectedTreasuryForCurrentEpoch), Math.floor(treasuryForCurrentEpoch));
    }
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
    return IntStream.range(210, 433).boxed();
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
            Arguments.of(216, 0.0)
    );
  }
  @ParameterizedTest
  @MethodSource("retiredPoolTestRange")
  void Test_calculateUnclaimedRefundsForRetiredPools(int epoch, double expectedUnclaimedRefunds) {
    double unclaimedRefunds = TreasuryCalculation.calculateUnclaimedRefundsForRetiredPools(epoch, koiosDataProvider);
    Assertions.assertEquals(expectedUnclaimedRefunds, unclaimedRefunds);
  }
}
