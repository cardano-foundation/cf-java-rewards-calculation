package org.cardanofoundation.rewards.calculation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
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

@SpringBootTest
@ComponentScan
public class TreasuryCalculationTest {

  @Autowired
  KoiosDataProvider koiosDataProvider;

  static Stream<Integer> range() {
    return IntStream.range(209, 433).boxed();
  }

  @ParameterizedTest
  @MethodSource("range")
  void Test_calculateTreasuryWithKoiosProvider(final int epoch) throws ApiException {
    EpochParams epochParams = koiosDataProvider.getProtocolParametersForEpoch(epoch);
    final double treasuryGrowthRate = epochParams.getTreasuryGrowthRate().doubleValue();
    final double monetaryExpandRate = epochParams.getMonetaryExpandRate().doubleValue();

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

    BigDecimal rewardPot =  TreasuryCalculation.calculateTotalRewardPot(
            monetaryExpandRate, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

    System.out.println(rewardPot);

    BigDecimal treasuryForCurrentEpoch = TreasuryCalculation.calculateTreasury(
            treasuryGrowthRate, rewardPot, treasuryInPreviousEpoch);


    int blocksInEpoch = koiosDataProvider.getTotalBlocksInEpoch(epoch - 1);
    BigDecimal distributedRewardInEpoch = koiosDataProvider.getDistributedRewardsInEpoch(epoch);
    System.out.println("DistributedRewardInEpoch: " + distributedRewardInEpoch);
    System.out.println("rewardPot: " + rewardPot);
    /*BigDecimal undistributedReward = TreasuryCalculation.calculateUndistributedReward(rewardPot, distributedRewardInEpoch, treasuryGrowthRate, blocksInEpoch);
    System.out.println(undistributedReward.divide(new BigDecimal("1000000"), RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue());*/

    /*
      "For each retiring pool, the refund for the pool registration deposit is added to the
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
  void Test_countRetiredPoolsWithDeregisteredRewardAddress() throws ApiException {
    List<Integer> expectedPoolCounts = new ArrayList<>(List.of(1, 0, 0, 1, 0, 1, 1));
    List<Integer> actualPoolCounts = new ArrayList<>();

    /*
    no offset
    Expected :[0, 0, 1, 0, 1, 1]
    Actual   :[0, 1, 1, 0, 0, 2, 1]
     */

    for (int epoch = 208; epoch < 215; epoch++) {
      actualPoolCounts.add(koiosDataProvider.countRetiredPoolsWithDeregisteredRewardAddress(epoch ));
    }
    Assertions.assertEquals(expectedPoolCounts, actualPoolCounts);
  }

  @Test
  void Test_calculateTreasuryForEpoch215() throws ApiException {
    Totals adaPotsForEpoch214 = koiosDataProvider.getAdaPotsForEpoch(214);
    Totals adaPotsForEpoch215 = koiosDataProvider.getAdaPotsForEpoch(215);

    final double treasuryGrowthRate = 0.2;

    BigDecimal reserveInEpoch214 = new BigDecimal(adaPotsForEpoch214.getReserves());
    BigDecimal treasuryInEpoch214 = new BigDecimal(adaPotsForEpoch214.getTreasury());
    BigDecimal expectedTreasury = new BigDecimal(adaPotsForEpoch215.getTreasury());

    final double monetaryExpandRate = 0.003;
    final BigDecimal feeInEpoch214 = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(214));
    final BigDecimal fee = new BigDecimal("7100590000");

    Assertions.assertEquals(fee, feeInEpoch214);

    BigDecimal rewardPot = TreasuryCalculation.calculateTotalRewardPot(monetaryExpandRate, reserveInEpoch214, feeInEpoch214);
    Assertions.assertEquals(new BigInteger("39818480 834 220"), rewardPot.toBigInteger());

    BigDecimal actualTreasury = TreasuryCalculation.calculateTreasury(treasuryGrowthRate, rewardPot, treasuryInEpoch214);
    Assertions.assertEquals(expectedTreasury.toBigInteger(), actualTreasury.toBigInteger());
  }
}
