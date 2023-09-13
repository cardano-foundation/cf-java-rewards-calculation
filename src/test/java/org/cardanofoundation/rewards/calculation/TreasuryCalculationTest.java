package org.cardanofoundation.rewards.calculation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.epoch.model.EpochParams;
import rest.koios.client.backend.api.network.model.Totals;

import static org.cardanofoundation.rewards.constants.RewardConstants.*;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class TreasuryCalculationTest {

  @Autowired
  KoiosDataProvider koiosDataProvider;

  static Stream<Integer> range() {
    return IntStream.range(215, 216).boxed();
  }

  @ParameterizedTest
    @MethodSource("range")
  void Test_calculateReserves(final int epoch) throws ApiException {
    Totals adaPotsForEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch);
    Totals adaPotsForNextEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch + 1);
    BigDecimal reserveInEpoch = new BigDecimal(adaPotsForEpoch.getReserves());

    BigDecimal inflationRate = BigDecimal.ONE.subtract(new BigDecimal("0.003"));
    BigDecimal calculatedReserves = reserveInEpoch.multiply(inflationRate);

    System.out.println("Difference in ADA: " + lovelaceToAda(
            calculatedReserves.subtract(new BigDecimal(adaPotsForNextEpoch.getReserves()))));

    Assertions.assertEquals(adaPotsForNextEpoch.getReserves(), calculatedReserves);
  }

  @ParameterizedTest
  @MethodSource("range")
  void Test_calculateTreasuryWithKoiosProvider(final int epoch) throws ApiException {
    EpochParams epochParams = koiosDataProvider.getProtocolParametersForEpoch(epoch);
    final double treasuryGrowthRate = epochParams.getTreasuryGrowthRate().doubleValue();
    final double monetaryExpandRate = epochParams.getMonetaryExpandRate().doubleValue();
    final double decentralizationParameter = epochParams.getDecentralisation().doubleValue();

    Totals adaPotsForPreviousEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch - 1);
    Totals adaPotsForCurrentEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch);

    // The Shelley era and the ada pot system started on mainnet in epoch 208.
    // Fee and treasury values are 0 for epoch 208.
    BigDecimal totalFeesForCurrentEpoch = new BigDecimal(0);
    if (epoch > 209) {
      // Comparing the total fee values with different explorers,
      // we found that Koios has an offset of 2 epochs.
      totalFeesForCurrentEpoch = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(epoch - 2));
    }

    BigDecimal reserveInPreviousEpoch = new BigDecimal(adaPotsForPreviousEpoch.getReserves());

    BigDecimal treasuryInPreviousEpoch = new BigDecimal(adaPotsForPreviousEpoch.getTreasury());
    BigDecimal expectedTreasuryForCurrentEpoch = new BigDecimal(adaPotsForCurrentEpoch.getTreasury());

    int totalBlocksInEpoch = koiosDataProvider.getTotalBlocksInEpoch(epoch - 1);
    BigDecimal rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
            monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

    BigDecimal treasuryForCurrentEpoch = TreasuryCalculation.calculateTreasury(
            treasuryGrowthRate, rewardPot, treasuryInPreviousEpoch);

    /*
      TODO: "For each retiring pool, the refund for the pool registration deposit is added to the
       pool's registered reward account, provided the reward account is still registered." -
       https://github.com/input-output-hk/cardano-ledger/blob/9e2f8151e3b9a0dde9faeb29a7dd2456e854427c/eras/shelley/formal-spec/epoch.tex#L546C9-L547C87
    */

    // int retiredPoolsWithDeregisteredRewardAddress = koiosDataProvider.countRetiredPoolsWithDeregisteredRewardAddress(epoch - 1);

    // BigDecimal deposit = new BigDecimal(DEPOSIT_POOL_REGISTRATION_IN_ADA);
    // treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(deposit.multiply(new BigDecimal(retiredPoolsWithDeregisteredRewardAddress)));

    int differenceInADA = expectedTreasuryForCurrentEpoch.subtract(treasuryForCurrentEpoch)
            .divide(new BigDecimal("1000000"), RoundingMode.HALF_UP)
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

    System.out.println("Difference in ADA: " + differenceInADA);

    if (differenceInADA > 0 && differenceInADA % 500 == 0) {
      int numberOfPools = differenceInADA / 500;
      System.out.println("Probably there was/were " + numberOfPools + " retired pool(s) with deregistered reward address in epoch " + epoch +
              ". That's why " + differenceInADA + " ADA was added to the treasury.");
      Assertions.assertEquals(0, 0);
    } else {
      Assertions.assertEquals(expectedTreasuryForCurrentEpoch.toBigInteger(), treasuryForCurrentEpoch.toBigInteger());
    }
  }

  @Test
  void Test_calculateTreasuryForEpoch214() throws ApiException {
    Totals adaPotsForEpoch213 = koiosDataProvider.getAdaPotsForEpoch(213);
    Totals adaPotsForEpoch214 = koiosDataProvider.getAdaPotsForEpoch(214);

    final double treasuryGrowthRate = 0.2;

    BigDecimal reserveInEpoch213 = new BigDecimal(adaPotsForEpoch213.getReserves());
    BigDecimal treasuryInEpoch213 = new BigDecimal(adaPotsForEpoch213.getTreasury());
    BigDecimal expectedTreasury = new BigDecimal(adaPotsForEpoch214.getTreasury());

    final double monetaryExpandRate = 0.003;
    final BigDecimal feeInEpoch214 = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(212));

    BigDecimal rewardPot = TreasuryCalculation.calculateTotalRewardPot(monetaryExpandRate, reserveInEpoch213, feeInEpoch214);
    BigDecimal actualTreasury = TreasuryCalculation.calculateTreasury(treasuryGrowthRate, rewardPot, treasuryInEpoch213);

    // One retired pool deregistered it's reward address before getting the deposit back
    // that's why it's added to the treasury.
    Assertions.assertEquals(expectedTreasury.toBigInteger(), actualTreasury.toBigInteger().add(DEPOSIT_POOL_REGISTRATION_IN_LOVELACE));
  }

  @Test
  void Test_calculateTreasuryForEpoch215() throws ApiException {
    Totals adaPotsForEpoch214 = koiosDataProvider.getAdaPotsForEpoch(216);
    Totals adaPotsForEpoch215 = koiosDataProvider.getAdaPotsForEpoch(216);

    final double treasuryGrowthRate = 0.2;

    BigDecimal reserveInEpoch214 =  new BigDecimal(adaPotsForEpoch214.getReserves());
    BigDecimal treasuryInEpoch214 = new BigDecimal(adaPotsForEpoch214.getTreasury());
    BigDecimal expectedTreasury = new BigDecimal(adaPotsForEpoch215.getTreasury());

    final double monetaryExpandRate = 0.003;
    final BigDecimal feeInEpoch214 = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(215));

    BigDecimal rewardPotFormula = TreasuryCalculation.calculateTotalRewardPot(monetaryExpandRate, reserveInEpoch214, feeInEpoch214);
    BigDecimal rewardPot = new BigDecimal("38890928000000");

    System.out.println("rewardPotFormula: " + rewardPotFormula + "\nrewardPot: " + rewardPot + "\ndiff: " + rewardPotFormula.subtract(rewardPot));

    BigDecimal actualTreasury = TreasuryCalculation.calculateTreasury(treasuryGrowthRate, rewardPot, treasuryInEpoch214);
    BigDecimal treasuryWithFormulaRewardPot = TreasuryCalculation.calculateTreasury(treasuryGrowthRate, rewardPotFormula, treasuryInEpoch214);
    System.out.println("Difference " + expectedTreasury.subtract(actualTreasury).divide(BigDecimal.valueOf(1000000), RoundingMode.HALF_UP));

    System.out.println(treasuryWithFormulaRewardPot);

    // One retired pool deregistered it's reward address before getting the deposit back
    // that's why it's added to the treasury.
    Assertions.assertEquals(expectedTreasury.toBigInteger(), actualTreasury.toBigInteger());
  }
}
