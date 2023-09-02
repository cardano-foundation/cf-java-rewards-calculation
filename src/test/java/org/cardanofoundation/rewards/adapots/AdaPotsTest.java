package org.cardanofoundation.rewards.adapots;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.service.impl.AdaPotsServiceImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.network.model.Totals;

@SpringBootTest
@ComponentScan
public class AdaPotsTest {

  @Autowired
  AdaPotsServiceImpl adaPotsService;

  @Autowired
  KoiosDataProvider koiosDataProvider;

  static Stream<Integer> range() {
    return IntStream.range(209, 433).boxed();
  }

  @ParameterizedTest
  @MethodSource("range")
  void Test_calculateTreasury(final int epoch) throws ApiException {
    final double treasuryGrowthRate = 0.2;
    final double monetaryExpandRate = 0.003;

    Totals adaPotsForPreviousEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch - 1);
    Totals adaPotsForCurrentEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch);

    BigDecimal reserveInPreviousEpoch = new BigDecimal(adaPotsForPreviousEpoch.getReserves());
    BigDecimal treasuryInPreviousEpoch = new BigDecimal(adaPotsForPreviousEpoch.getTreasury());
    BigDecimal expectedTreasuryForCurrentEpoch = new BigDecimal(adaPotsForCurrentEpoch.getTreasury());

    // Documentation: Comparing the total fee values with different explorers,
    //                we found that Koios has an offset of 2 epochs.
    BigDecimal totalFeesForCurrentEpoch = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(epoch - 2));
    var rewardPot = adaPotsService.calculateRewards(monetaryExpandRate, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

    BigDecimal treasuryForCurrentEpoch = adaPotsService.calculateTreasury(treasuryGrowthRate, rewardPot, treasuryInPreviousEpoch);
    Assertions.assertEquals(expectedTreasuryForCurrentEpoch.toBigInteger(), treasuryForCurrentEpoch.toBigInteger());
  }

  @Test
  void Test_calculateTreasuryForEpoch212() throws ApiException {
    Totals adaPotsForEpoch211 = koiosDataProvider.getAdaPotsForEpoch(211);

    final double treasuryGrowthRate = 0.2;

    BigDecimal reserveInEpoch211 = new BigDecimal("13270236767315870");
    BigDecimal treasuryInEpoch211 = new BigDecimal("24275595982960");
    BigDecimal expectedTreasury = new BigDecimal("32239292149804");

    Assertions.assertEquals(treasuryInEpoch211, new BigDecimal(adaPotsForEpoch211.getTreasury()));
    Assertions.assertEquals(reserveInEpoch211, new BigDecimal(adaPotsForEpoch211.getReserves()));

    final double monetaryExpandRate = 0.003;
    final BigDecimal feeInEpoch211 = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(210));
    final BigDecimal fee = new BigDecimal("7770532273");

    Assertions.assertEquals(fee, feeInEpoch211);

    BigDecimal rewardPot = adaPotsService.calculateRewards(monetaryExpandRate, reserveInEpoch211, feeInEpoch211);

    /*final int totalBlocksInEpoch211 = koiosDataProvider.getTotalBlocksInEpoch(211);

    var slotPerEpoch = new BigDecimal(RewardConstants.EXPECTED_SLOT_PER_EPOCH);
    var eta =
            new BigDecimal(totalBlocksInEpoch211)
                    .divide(
                            BigDecimal.ONE
                                    .subtract(new BigDecimal("0.78"))
                                    .multiply(slotPerEpoch),
                            30,
                            RoundingMode.DOWN);

    rewardPot = rewardPot.multiply(eta);*/
    Assertions.assertEquals(new BigInteger("39818480834220"), rewardPot.toBigInteger());

    BigDecimal actualTreasury = adaPotsService.calculateTreasury(treasuryGrowthRate, rewardPot, treasuryInEpoch211);
    Assertions.assertEquals(expectedTreasury.toBigInteger(), actualTreasury.toBigInteger());
  }

  @Test
  void Test_calculateRewardsForEpoch215() {
    final double monetaryExpandRate = 0.003;
    final BigDecimal reserveOfEpoch208 = new BigDecimal("13888022853000");

    BigDecimal feeOfEpoch208 = new BigDecimal("0");
    final BigDecimal feeOfFirstBlockOfEpoch208 = new BigDecimal("0");
    feeOfEpoch208 = feeOfEpoch208.subtract(feeOfFirstBlockOfEpoch208);

    BigDecimal expectedRewards = new BigDecimal("593536826");
    BigDecimal actualRewards = adaPotsService.calculateRewards(monetaryExpandRate, reserveOfEpoch208, feeOfEpoch208);
    Assertions.assertEquals(expectedRewards, actualRewards);
  }

  @Test
  void Test_calculateRewardsForEpoch210() {
    final double monetaryExpandRate = 0.003;
    final BigDecimal reserveOfEpoch209 = new BigDecimal("13286160713");

    BigDecimal feeOfEpoch209 = new BigDecimal("10670559402");
    final BigDecimal feeOfFirstBlockOfEpoch209 = new BigDecimal("0.347194");
    feeOfEpoch209 = feeOfEpoch209.subtract(feeOfFirstBlockOfEpoch209);

    BigDecimal expectedRewards = new BigDecimal("277915861");
    BigDecimal actualRewards = adaPotsService.calculateRewards(monetaryExpandRate, reserveOfEpoch209, feeOfEpoch209);
    Assertions.assertEquals(expectedRewards, actualRewards);
  }
}
