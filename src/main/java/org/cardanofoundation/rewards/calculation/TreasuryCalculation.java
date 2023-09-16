package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.cardanofoundation.rewards.entity.TreasuryCalculationResult;

import static org.cardanofoundation.rewards.constants.RewardConstants.EXPECTED_SLOT_PER_EPOCH;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

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

  public static TreasuryCalculationResult calculateTreasuryForEpoch(int epoch, DataProvider dataProvider) {
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

    return TreasuryCalculationResult.builder()
            .calculatedTreasury(treasuryForCurrentEpoch)
            .actualTreasury(expectedTreasuryForCurrentEpoch)
            .epoch(epoch)
            .totalRewardPot(rewardPot)
            .build();
  }
}
