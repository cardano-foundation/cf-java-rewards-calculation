package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.calculation.enums.AccountUpdateAction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.EXPECTED_SLOT_PER_EPOCH;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.POOL_DEPOSIT_IN_LOVELACE;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.multiplyAndFloor;

public class TreasuryCalculation {

  /*
   * Calculate the reward pot for epoch e with the formula:
   *
   * rewards(e) = floor(monetary_expand_rate * eta * reserve(e - 1) + fee(e - 1))
   * rewards(e) = 0, if e < 209
   */
  public static BigInteger calculateTotalRewardPotWithEta(double monetaryExpandRate, int totalBlocksInEpochByPools,
                                                          double decentralizationParameter, BigInteger reserve, BigInteger fee) {
    BigDecimal eta = calculateEta(totalBlocksInEpochByPools, decentralizationParameter);
    return multiplyAndFloor(reserve, monetaryExpandRate, eta).add(fee);
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
  private static BigDecimal calculateEta(int totalBlocksInEpochByPools, double decentralizationParameter) {
    // shelley-delegation.pdf 5.4.3

    if (decentralizationParameter >= 0.8) {
      return BigDecimal.ONE;
    }

    // The number of expected blocks will be the number of slots per epoch times the active slots coefficient
    BigDecimal activeSlotsCoeff = new BigDecimal("0.05"); // See: Non-Updatable Parameters: https://cips.cardano.org/cips/cip9/

    // decentralizationParameter is the proportion of blocks that are expected to be produced by stake pools
    // instead of the OBFT (Ouroboros Byzantine Fault Tolerance) nodes. It was introduced close before the Shelley era:
    // https://github.com/input-output-hk/cardano-ledger/commit/c4f10d286faadcec9e4437411bce9c6c3b6e51c2
    BigDecimal expectedBlocksInNonOBFTSlots = new BigDecimal(EXPECTED_SLOT_PER_EPOCH )
            .multiply(activeSlotsCoeff).multiply (new BigDecimal(1 - decentralizationParameter));

    // eta is the ratio between the number of blocks that have been produced during the epoch, and
    // the expectation value of blocks that should have been produced during the epoch under
    // ideal conditions.
    MathContext mathContext = new MathContext(32);
    return new BigDecimal(totalBlocksInEpochByPools).divide(expectedBlocksInNonOBFTSlots, mathContext).min(BigDecimal.ONE);
  }

  /*
    "For each retiring pool, the refund for the pool registration deposit is added to the
    pool's registered reward account, provided the reward account is still registered." -
    https://github.com/input-output-hk/cardano-ledger/blob/9e2f8151e3b9a0dde9faeb29a7dd2456e854427c/eras/shelley/formal-spec/epoch.tex#L546C9-L547C87
   */
  public static BigInteger calculateUnclaimedRefundsForRetiredPools(List<PoolDeregistration> retiredPools,
                                                                    List<AccountUpdate> latestAccountUpdates) {
    BigInteger refunds = BigInteger.ZERO;

    if (retiredPools.size() > 0) {
      for (AccountUpdate lastAccountUpdate : latestAccountUpdates) {
        if (lastAccountUpdate.getAction() == AccountUpdateAction.DEREGISTRATION) {
          refunds = refunds.add(POOL_DEPOSIT_IN_LOVELACE);
        }
      }
    }

    return refunds;
  }
}
