package org.cardanofoundation.rewards.computation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;
import org.cardanofoundation.rewards.enums.MirPot;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.cardanofoundation.rewards.constants.RewardConstants.*;
import static org.cardanofoundation.rewards.util.BigNumberUtils.*;

public class TreasuryComputation {

  /*
   * Calculate the reward pot for epoch e with the formula:
   *
   * rewards(e) = floor(monetary_expand_rate * eta * reserve(e - 1) + fee(e - 1))
   * rewards(e) = 0, if e < 209
   */
  public static BigInteger calculateTotalRewardPotWithEta(double monetaryExpandRate, int totalBlocksInEpochByPools,
                                                          double decentralizationParameter, BigInteger reserve, BigInteger fee) {
    double eta = calculateEta(totalBlocksInEpochByPools, decentralizationParameter);
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
    // The Shelley era and the ada pot system started on mainnet in epoch 208.
    // Fee and treasury values are 0 for epoch 208.
    if (epoch == 208) {
        return TreasuryCalculationResult.builder()
                .treasury(BigInteger.ZERO)
                .epoch(epoch)
                .totalRewardPot(BigInteger.ZERO)
                .treasuryWithdrawals(BigInteger.ZERO)
                .build();
    }

    double treasuryGrowthRate = 0.2;
    double monetaryExpandRate = 0.003;
    double decentralizationParameter = 1;

    AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
    BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
    int totalBlocksInEpoch = 0;

    /* We need to use the epoch info 2 epochs before as shelley starts in epoch 208 it will be possible to get
       those values from epoch 210 onwards. Before that we need to use the genesis values, but they are not
       needed anyway if the decentralization parameter is > 0.8.
       See: shelley-delegation.pdf 5.4.3 */
    if (epoch > 209) {
      ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
      Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);
      totalFeesForCurrentEpoch = epochInfo.getFees();
      treasuryGrowthRate = protocolParameters.getTreasuryGrowRate();
      monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
      decentralizationParameter = protocolParameters.getDecentralisation();

      totalBlocksInEpoch = epochInfo.getBlockCount();

      if (decentralizationParameter < 0.8 && decentralizationParameter > 0.0) {
        totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
      }
    }

    BigInteger reserveInPreviousEpoch = adaPotsForPreviousEpoch.getReserves();

    BigInteger treasuryInPreviousEpoch = adaPotsForPreviousEpoch.getTreasury();
    BigInteger rewardPot = TreasuryComputation.calculateTotalRewardPotWithEta(
            monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

    BigInteger treasuryCut = multiplyAndFloor(rewardPot, treasuryGrowthRate);
    BigInteger treasuryForCurrentEpoch = treasuryInPreviousEpoch.add(treasuryCut);

    // The sum of all the refunds attached to unregistered reward accounts are added to the
    // treasury (see: Pool Reap Transition, p.53, figure 40, shely-ledger.pdf)

    List<PoolDeregistration> retiredPools = dataProvider.getRetiredPoolsInEpoch(epoch);
    List<AccountUpdate> accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(
            retiredPools.stream().map(PoolDeregistration::getRewardAddress).toList(), epoch - 1);
    treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(TreasuryComputation.calculateUnclaimedRefundsForRetiredPools(retiredPools, accountUpdates));

    // Check if there was a MIR Certificate in the previous epoch
    BigInteger treasuryWithdrawals = BigInteger.ZERO;
    List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
    for (MirCertificate mirCertificate : mirCertificates) {
      if (mirCertificate.getPot() == MirPot.TREASURY) {
        treasuryWithdrawals = treasuryWithdrawals.add(mirCertificate.getTotalRewards());
      }
    }
    treasuryForCurrentEpoch = treasuryForCurrentEpoch.subtract(treasuryWithdrawals);

    return TreasuryCalculationResult.builder()
            .treasury(treasuryForCurrentEpoch)
            .epoch(epoch)
            .totalRewardPot(rewardPot)
            .treasuryWithdrawals(treasuryWithdrawals)
            .build();
  }

  /*
    "For each retiring pool, the refund for the pool registration deposit is added to the
    pool's registered reward account, provided the reward account is still registered." -
    https://github.com/input-output-hk/cardano-ledger/blob/9e2f8151e3b9a0dde9faeb29a7dd2456e854427c/eras/shelley/formal-spec/epoch.tex#L546C9-L547C87
   */
  public static BigInteger calculateUnclaimedRefundsForRetiredPools(List<PoolDeregistration> retiredPools,
                                                                    List<AccountUpdate> accountUpdates) {
    BigInteger refunds = BigInteger.ZERO;

    if (retiredPools.size() > 0) {
      // Order list by unix block time
      accountUpdates = accountUpdates.stream().filter(update ->
              update.getAction().equals(AccountUpdateAction.DEREGISTRATION)
                      || update.getAction().equals(AccountUpdateAction.REGISTRATION)).sorted(
              Comparator.comparing(AccountUpdate::getUnixBlockTime).reversed()).toList();

      // only hold the latest account update for each reward address
      // preventing the case of an unregistered reward address becoming registered again
      List<AccountUpdate> latestAccountUpdates = new ArrayList<>();
      for (AccountUpdate accountUpdate : accountUpdates) {
        if (latestAccountUpdates.stream().map(AccountUpdate::getStakeAddress).noneMatch(stakeAddress -> stakeAddress.equals(accountUpdate.getStakeAddress()))) {
          latestAccountUpdates.add(accountUpdate);
        }
      }

      for (AccountUpdate lastAccountUpdate : latestAccountUpdates) {
        if (lastAccountUpdate.getAction() == AccountUpdateAction.DEREGISTRATION) {
          refunds = refunds.add(POOL_DEPOSIT_IN_LOVELACE);
        }
      }
    }

    return refunds;
  }
}
