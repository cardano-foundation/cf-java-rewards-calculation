package org.cardanofoundation.rewards.calculation;

import static org.cardanofoundation.rewards.constants.RewardConstants.EXPECTED_SLOT_PER_EPOCH;

public class TreasuryCalculation {

  /*
   * Calculate the treasury for epoch e with the formula:
   *
   * treasury(e) = treasury_growth_rate * reward_pot + treasury(e - 1)
   * treasury(e) = 0, if e < 209
   */
  public static double calculateTreasury(double treasuryGrowRate, double rewardPot, double lastTreasury) {
    return rewardPot * treasuryGrowRate + lastTreasury;
  }

  /*
   * Calculate the reward pot for epoch e with the formula:
   *
   * rewards(e) = floor(monetary_expand_rate * eta * reserve(e - 1) + fee(e - 1))
   * rewards(e) = 0, if e < 209
   */
  public static double calculateTotalRewardPotWithEta(double monetaryExpandRate, int totalBlockInEpoch,
                                                         double decentralizationParameter, double reserve, double fee) {
    double eta = calculateEta(totalBlockInEpoch, decentralizationParameter);
    return Math.floor(reserve * monetaryExpandRate * eta + fee);
  }

  /*
  * Calculate the eta using the decentralisation parameter and the formula:
  * eta(totalBlocksInEpoch, decentralisation) = 1, if decentralisation >= 0.8, otherwise
  * eta(totalBlocksInEpoch, decentralisation) =
  *   min(1, totalBlocksInEpoch / ((1 - decentralisation) * expectedSlotPerEpoch))
  */
  private static double calculateEta(int totalBlocksInEpoch, double decentralizationParameter) {
    // shelley-delegation.pdf 5.4.3

    if (decentralizationParameter >= 0.8) {
      return 1.0;
    }

    // The number of expected blocks will be the number of slots per epoch times the active slots coefficient
    double activeSlotsCoeff = 0.05; // See: Non-Updatable Parameters: https://cips.cardano.org/cips/cip9/
    double expectedBlocksInNonOBFTSlots = Math.floor(EXPECTED_SLOT_PER_EPOCH * activeSlotsCoeff * (1 - decentralizationParameter));

    // eta is the ratio between the number of blocks that have been produced during the epoch, and
    // the expectation value of blocks that should have been produced during the epoch under
    // ideal conditions.
    return Math.min(1, totalBlocksInEpoch / expectedBlocksInNonOBFTSlots);
  }
}