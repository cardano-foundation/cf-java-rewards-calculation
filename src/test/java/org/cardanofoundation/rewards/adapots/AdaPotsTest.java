package org.cardanofoundation.rewards.adapots;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.service.impl.AdaPotsServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ComponentScan;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.network.model.Totals;

import static org.cardanofoundation.rewards.constants.RewardConstants.DEPOSIT_POOL_REGISTRATION_IN_ADA;

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
  @Disabled
  void Test_calculateTreasury(final int epoch) throws ApiException {
    final double treasuryGrowthRate = 0.2;
    final double monetaryExpandRate = 0.003;

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

    BigDecimal rewardPot = adaPotsService.calculateTotalRewardPot(monetaryExpandRate, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

    BigDecimal treasuryForCurrentEpoch = adaPotsService.calculateTreasury(treasuryGrowthRate, rewardPot, treasuryInPreviousEpoch);

    /*
      "For each retiring pool, the refund for the pool registration deposit is added to the
      pool's registered reward account, provided the reward account is still registered." -
      https://github.com/input-output-hk/cardano-ledger/blob/9e2f8151e3b9a0dde9faeb29a7dd2456e854427c/eras/shelley/formal-spec/epoch.tex#L546C9-L547C87
    */
    int retiredPoolsWithDeregisteredRewardAddress = koiosDataProvider.countRetiredPoolsWithDeregisteredRewardAddress(epoch - 1);

    BigDecimal deposit = new BigDecimal(DEPOSIT_POOL_REGISTRATION_IN_ADA);
    treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(deposit.multiply(new BigDecimal(retiredPoolsWithDeregisteredRewardAddress)));

    Assertions.assertEquals(expectedTreasuryForCurrentEpoch.toBigInteger(), treasuryForCurrentEpoch.toBigInteger());
  }

  @Test
  void Test_countRetiredPoolsWithDeregisteredRewardAddress() throws ApiException {
    int poolCountinEpoch209 = koiosDataProvider.countRetiredPoolsWithDeregisteredRewardAddress(209);
    int poolCountinEpoch210 = koiosDataProvider.countRetiredPoolsWithDeregisteredRewardAddress(210);
    int poolCountinEpoch211 = koiosDataProvider.countRetiredPoolsWithDeregisteredRewardAddress(211);
    Assertions.assertEquals(0, poolCountinEpoch209);
    Assertions.assertEquals(1, poolCountinEpoch210);
    Assertions.assertEquals(0, poolCountinEpoch211);
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

    BigDecimal rewardPot = adaPotsService.calculateTotalRewardPot(monetaryExpandRate, reserveInEpoch211, feeInEpoch211);

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
}
