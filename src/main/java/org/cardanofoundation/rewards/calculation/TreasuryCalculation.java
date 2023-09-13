package org.cardanofoundation.rewards.calculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.cardanofoundation.rewards.constants.RewardConstants.EXPECTED_SLOT_PER_EPOCH;

public class TreasuryCalculation {

  /*
   * Calculate the treasury for epoch e with the formula:
   *
   * treasury(e) = treasury_growth_rate * (reward_pot * 0.2) + treasury(e - 1)
   * treasury(e) = 0, if e < 209
   */
  public static BigDecimal calculateTreasury(double treasuryGrowRate, BigDecimal rewardPot, BigDecimal lastTreasury) {
    BigDecimal treasury = rewardPot.multiply(new BigDecimal(treasuryGrowRate));
    return treasury.add(lastTreasury);
  }

  /*
   * Calculate the reward pot for epoch e with the formula:
   *
   * rewards(e) = monetary_expand_rate * eta * reserve(e - 1) + fee(e - 1)
   * rewards(e) = 0, if e < 209
   */
  public static BigDecimal calculateTotalRewardPotWithEta(double monetaryExpandRate, int totalBlockInEpoch,
                                                         double decentralizationParameter, BigDecimal reserve, BigDecimal fee) {
    BigDecimal eta = calculateEta(totalBlockInEpoch, decentralizationParameter);
    System.out.println(eta);
    BigDecimal totalRewardPot = reserve.multiply(new BigDecimal(monetaryExpandRate).multiply(eta));
    return totalRewardPot.add(fee).setScale(0, RoundingMode.FLOOR);
  }

  /*
   * Calculate the reward pot for epoch e with the formula:
   *
   * rewards(e) = monetary_expand_rate * reserve(e - 1) + fee(e - 1)
   * rewards(e) = 0, if e < 209
   */
  public static BigDecimal calculateTotalRewardPot(double monetaryExpandRate, BigDecimal reserve, BigDecimal fee) {
    BigDecimal totalRewardPot = reserve.multiply(new BigDecimal(monetaryExpandRate));
    return totalRewardPot.add(fee);
  }

  /*
  * Calculate the eta using the decentralisation parameter and the formula:
  * eta(totalBlocksInEpoch, decentralisation) = 1, if decentralisation >= 0.8, otherwise
  * eta(totalBlocksInEpoch, decentralisation) =
  *   min(1, totalBlocksInEpoch / ((1 - decentralisation) * expectedSlotPerEpoch))
  */
  private static BigDecimal calculateEta(int totalBlocksInEpoch, double decentralizationParameter) {
    // shelley-delegation.pdf 5.4.3

    if (decentralizationParameter >= 0.8) {
      return BigDecimal.ONE;
    }

    // The number of expected blocks will be the number of slots per epoch times the active slots coefficient
    double activeSlotsCoeff = 0.05; // See: Non-Updatable Parameters: https://cips.cardano.org/cips/cip9/
    double expectedBlocksInNonOBFTSlots = Math.floor(EXPECTED_SLOT_PER_EPOCH * activeSlotsCoeff * (1 - decentralizationParameter));

    // eta is the ratio between the number of blocks that have been produced during the epoch, and
    // the expectation value of blocks that should have been produced during the epoch under
    // ideal conditions.
    BigDecimal eta = new BigDecimal(totalBlocksInEpoch).divide(new BigDecimal(expectedBlocksInNonOBFTSlots), 30, RoundingMode.HALF_UP);;
    return eta.min(BigDecimal.ONE);
  }
}
