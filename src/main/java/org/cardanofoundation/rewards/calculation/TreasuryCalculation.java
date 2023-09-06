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
  public static BigDecimal calculateTotalRewardPotWithEta(double monetaryExpandRate,
                                                   int totalBlockInEpoch, BigDecimal reserve, BigDecimal fee) {
    BigDecimal eta = calculateEta(totalBlockInEpoch);
    System.out.println("eta: " + eta);
    BigDecimal totalRewardPot = reserve.multiply(new BigDecimal(monetaryExpandRate).multiply(eta));
    return totalRewardPot.add(fee);
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
  private static BigDecimal calculateEta(int totalBlocksInEpoch) {
    // shelley-delegation.pdf 5.4.3

    // The number of expected blocks will be the number of slots per epoch times the active slots coefficient
    double activeSlotsCoeff = 0.05; // See: Non-Updatable Parameters: https://cips.cardano.org/cips/cip9/
    double expectedBlocks = EXPECTED_SLOT_PER_EPOCH * activeSlotsCoeff;

    // eta is the ratio between the number of blocks that have been produced during the epoch, and
    // the expectation value18 of blocks that should have been produced during the epoch under
    // ideal conditions.
    BigDecimal eta = new BigDecimal(totalBlocksInEpoch).divide(new BigDecimal(expectedBlocks), 18, RoundingMode.DOWN);;
    return eta.min(BigDecimal.ONE);
  }

  /*
    https://github.com/input-output-hk/cardano-ledger/blob/453d19da239d0eb123a98aabac2f74f5bfdb41f7/eras/shelley/formal-spec/epoch.tex#L1393

    As given by $\fun{maxPool}$, each pool can receive a maximal amount, determined by its
    performance. The difference between the maximal amount and the actual amount received is
    added to the amount moved to the treasury.
   */
  public static BigDecimal calculateUndistributedReward(BigDecimal totalRewardPot, BigDecimal distributedReward, double treasuryGrowRate, int totalBlockInEpoch) {
    BigDecimal eta = calculateEta(totalBlockInEpoch);
    System.out.println("eta: " + eta);
    BigDecimal stakePoolRewardsPot = totalRewardPot.multiply(BigDecimal.ONE.subtract(eta.multiply(new BigDecimal(treasuryGrowRate))));
    return stakePoolRewardsPot.subtract(distributedReward);
  }
}
