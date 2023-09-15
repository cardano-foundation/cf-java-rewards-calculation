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
  public static double calculateTotalRewardPotWithEta(double monetaryExpandRate, int totalBlocksInEpochByPools,
                                                         double decentralizationParameter, double reserve, double fee) {
    double eta = calculateEta(totalBlocksInEpochByPools, decentralizationParameter);
    return Math.floor(reserve * monetaryExpandRate * eta + fee);
  }

  /*
  * Calculate eta using the decentralisation parameter and the formula:
  *
  * eta(totalBlocksInEpochMadeByPools, decentralisation) = 1, if decentralisation >= 0.8, otherwise
  * eta(totalBlocksInEpochMadeByPools, decentralisation) =
  *   min(1, totalBlocksInEpochMadeByPools / ((1 - decentralisation) * expectedSlotPerEpoch * activeSlotsCoeff))
  *
  * See: https://github.com/input-output-hk/cardano-ledger/commit/c4f10d286faadcec9e4437411bce9c6c3b6e51c2
  */
  private static double calculateEta(int totalBlocksInEpochByPools, double decentralizationParameter) {
    // shelley-delegation.pdf 5.4.3

    if (decentralizationParameter >= 0.8) {
      return 1.0;
    }

    // The number of expected blocks will be the number of slots per epoch times the active slots coefficient
    double activeSlotsCoeff = 0.05; // See: Non-Updatable Parameters: https://cips.cardano.org/cips/cip9/

    // decentralizationParameter is the proportion of blocks that are expected to be produced by stake pools
    // instead of the OBFT (Ouroboros Byzantine Fault Tolerance) nodes. It was introduced close before the Shelley era:
    // https://github.com/input-output-hk/cardano-ledger/commit/c4f10d286faadcec9e4437411bce9c6c3b6e51c2
    double expectedBlocksInNonOBFTSlots = EXPECTED_SLOT_PER_EPOCH * activeSlotsCoeff * (1 - decentralizationParameter);

    // eta is the ratio between the number of blocks that have been produced during the epoch, and
    // the expectation value of blocks that should have been produced during the epoch under
    // ideal conditions.
    return Math.min(1, totalBlocksInEpochByPools / expectedBlocksInNonOBFTSlots);
  }
}
