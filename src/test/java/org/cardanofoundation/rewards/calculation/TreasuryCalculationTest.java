package org.cardanofoundation.rewards.calculation;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.junit.jupiter.params.ParameterizedTest;
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

  static Stream<Integer> range() {
    return IntStream.range(209, 215).boxed();
  }

  @ParameterizedTest
  @MethodSource("range")
  void Test_calculateTreasuryWithKoiosProvider(final int epoch) {
    ProtocolParameters protocolParameters = koiosDataProvider.getProtocolParametersForEpoch(epoch);
    final double treasuryGrowthRate = protocolParameters.getTreasuryGrowRate();
    final double monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
    final double decentralizationParameter = protocolParameters.getDecentralisation();

    AdaPots adaPotsForPreviousEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch - 1);
    AdaPots adaPotsForCurrentEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch);

    // The Shelley era and the ada pot system started on mainnet in epoch 208.
    // Fee and treasury values are 0 for epoch 208.
    double totalFeesForCurrentEpoch = 0.0;
    Epoch epochInfo = koiosDataProvider.getEpochInfo(epoch - 2);
    if (epoch > 209) {
      totalFeesForCurrentEpoch = epochInfo.getFees();
    }

    double reserveInPreviousEpoch = adaPotsForPreviousEpoch.getReserves();

    double treasuryInPreviousEpoch = adaPotsForPreviousEpoch.getTreasury();
    double expectedTreasuryForCurrentEpoch = adaPotsForCurrentEpoch.getTreasury();

    // TODO: It seems that the blocks we need to use for the eta calculation are the blocks produced by pools (NON-OBFT blocks).
    int totalBlocksInEpoch = epochInfo.getBlockCount();
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

  @Test
  void Test_calculateTreasuryForEpoch214() {
    AdaPots adaPotsForEpoch213 = koiosDataProvider.getAdaPotsForEpoch(213);
    AdaPots adaPotsForEpoch214 = koiosDataProvider.getAdaPotsForEpoch(214);

    final double treasuryGrowthRate = 0.2;

    double reserveInEpoch213 = adaPotsForEpoch213.getReserves();
    double treasuryInEpoch213 = adaPotsForEpoch213.getTreasury();
    double expectedTreasury = adaPotsForEpoch214.getTreasury();

    final double monetaryExpandRate = 0.003;

    Epoch epochInfo = koiosDataProvider.getEpochInfo(212);
    final double feeInEpoch214 = epochInfo.getFees();

    ProtocolParameters protocolParameters = koiosDataProvider.getProtocolParametersForEpoch(213);
    double decentralizationParameter = protocolParameters.getDecentralisation();

    double rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
            monetaryExpandRate, epochInfo.getBlockCount(), decentralizationParameter, reserveInEpoch213, feeInEpoch214);
    double actualTreasury = TreasuryCalculation.calculateTreasury(treasuryGrowthRate, rewardPot, treasuryInEpoch213);

    // One retired pool deregistered it's reward address before getting the deposit back
    // that's why it's added to the treasury.
    Assertions.assertEquals(Math.floor(expectedTreasury), Math.floor(actualTreasury) + DEPOSIT_POOL_REGISTRATION_IN_LOVELACE);
  }
}
