package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.calculation.enums.MirPot;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.*;

public class TreasuryCalculation {

  public static TreasuryCalculationResult calculateTreasuryInEpoch(int epoch, ProtocolParameters protocolParameters,
                                                                   AdaPots adaPotsForPreviousEpoch, Epoch epochInfo,
                                                                   HashSet<String> rewardAddressesOfRetiredPools,
                                                                   List<MirCertificate> mirCertificates,
                                                                   final HashSet<String> deregisteredAccounts,
                                                                   final HashSet<String> registeredAccountsUntilNow,
                                                                   BigInteger unspendableEarnedRewards,
                                                                   NetworkConfig networkConfig) {
    // The Shelley era and the ada pot system started on mainnet in epoch 208.
    // Fee and treasury values are 0 for epoch 208.
    if (epoch <= networkConfig.getShelleyStartEpoch()) {
      return TreasuryCalculationResult.builder()
              .treasury(BigInteger.ZERO)
              .epoch(epoch)
              .totalRewardPot(BigInteger.ZERO)
              .treasuryWithdrawals(BigInteger.ZERO)
              .unspendableEarnedRewards(BigInteger.ZERO)
              .unclaimedRefunds(BigInteger.ZERO)
              .build();
    }

    BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
    int totalBlocksInEpoch = 0;

    BigDecimal treasuryGrowthRate = protocolParameters.getTreasuryGrowRate();
    BigDecimal monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
    BigDecimal decentralizationParameter = protocolParameters.getDecentralisation();

    if (epochInfo != null) {
      totalFeesForCurrentEpoch = epochInfo.getFees();
      totalBlocksInEpoch = epochInfo.getBlockCount();
      if (isLower(decentralizationParameter, BigDecimal.valueOf(0.8)) && isHigher(decentralizationParameter, BigDecimal.ZERO)) {
        totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
      }
    }

    final BigInteger reserveInPreviousEpoch = adaPotsForPreviousEpoch.getReserves();
    final BigInteger treasuryInPreviousEpoch = adaPotsForPreviousEpoch.getTreasury();

    final BigInteger totalRewardPot = calculateTotalRewardPotWithEta(
            monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch, networkConfig);

    final BigInteger treasuryCut = multiplyAndFloor(totalRewardPot, treasuryGrowthRate);
    BigInteger treasuryForCurrentEpoch = treasuryInPreviousEpoch.add(treasuryCut);

    BigInteger unclaimedRefunds = BigInteger.ZERO;
    if (rewardAddressesOfRetiredPools.size() > 0) {
      HashSet<String> deregisteredRewardAccounts = deregisteredAccounts.stream()
              .filter(rewardAddressesOfRetiredPools::contains).collect(Collectors.toCollection(HashSet::new));
      List<String> ownerAccountsRegisteredInThePast = registeredAccountsUntilNow.stream()
              .filter(rewardAddressesOfRetiredPools::contains).toList();

      unclaimedRefunds = calculateUnclaimedRefundsForRetiredPools(rewardAddressesOfRetiredPools, deregisteredRewardAccounts, ownerAccountsRegisteredInThePast, networkConfig);
      treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(unclaimedRefunds);
    }

    BigInteger treasuryWithdrawals = BigInteger.ZERO;
    for (MirCertificate mirCertificate : mirCertificates) {
      if (mirCertificate.getPot() == MirPot.TREASURY) {
        treasuryWithdrawals = treasuryWithdrawals.add(mirCertificate.getTotalRewards());
      }
    }

    treasuryForCurrentEpoch = treasuryForCurrentEpoch.subtract(treasuryWithdrawals);
    treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(unspendableEarnedRewards);

    return TreasuryCalculationResult.builder()
            .treasury(treasuryForCurrentEpoch)
            .epoch(epoch)
            .totalRewardPot(totalRewardPot)
            .treasuryWithdrawals(treasuryWithdrawals)
            .unspendableEarnedRewards(unspendableEarnedRewards)
            .unclaimedRefunds(unclaimedRefunds)
            .build();
    }

  /*
   * Calculate the reward pot for epoch e with the formula:
   *
   * rewards(e) = floor(monetary_expand_rate * eta * reserve(e - 1) + fee(e - 1))
   * rewards(e) = 0, if e < 209
   */
  public static BigInteger calculateTotalRewardPotWithEta(BigDecimal monetaryExpandRate, int totalBlocksInEpochByPools,
                                                          BigDecimal decentralizationParameter, BigInteger reserve, BigInteger fee, NetworkConfig networkConfig) {
    BigDecimal eta = calculateEta(totalBlocksInEpochByPools, decentralizationParameter, networkConfig);
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
  private static BigDecimal calculateEta(int totalBlocksInEpochByPools, BigDecimal decentralizationParameter, NetworkConfig networkConfig) {
    // shelley-delegation.pdf 5.4.3

    BigDecimal decentralisationThreshold = new BigDecimal("0.8");
    if (decentralizationParameter.compareTo(decentralisationThreshold) >= 0) {
      return BigDecimal.ONE;
    }

    // The number of expected blocks will be the number of slots per epoch times the active slots coefficient
    BigDecimal activeSlotsCoeff = BigDecimal.valueOf(networkConfig.getActiveSlotCoefficient()); // See: Non-Updatable Parameters: https://cips.cardano.org/cips/cip9/

    // decentralizationParameter is the proportion of blocks that are expected to be produced by stake pools
    // instead of the OBFT (Ouroboros Byzantine Fault Tolerance) nodes. It was introduced close before the Shelley era:
    // https://github.com/input-output-hk/cardano-ledger/commit/c4f10d286faadcec9e4437411bce9c6c3b6e51c2
    BigDecimal expectedBlocksInNonOBFTSlots = new BigDecimal(networkConfig.getExpectedSlotsPerEpoch())
            .multiply(activeSlotsCoeff).multiply(BigDecimal.ONE.subtract(decentralizationParameter));

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
  public static BigInteger calculateUnclaimedRefundsForRetiredPools(HashSet<String> rewardAddressesOfRetiredPools,
                                                                    HashSet<String> deregisteredRewardAccounts,
                                                                    List<String> ownerAccountsRegisteredInThePast,
                                                                    NetworkConfig networkConfig) {
    BigInteger unclaimedRefunds = BigInteger.ZERO;
    if (rewardAddressesOfRetiredPools.size() > 0) {
    /* Check if the reward address of the retired pool has been unregistered before
       or if the reward address has been unregistered after the randomness stabilization window
       or if the reward address has not been registered at all */
      for (String rewardAddress : rewardAddressesOfRetiredPools) {
        if (deregisteredRewardAccounts.contains(rewardAddress) ||
                !ownerAccountsRegisteredInThePast.contains(rewardAddress)) {
          // If the reward address has been unregistered, the deposit can not be returned
          // and will be added to the treasury instead (Pool Reap see: shelley-ledger.pdf p.53)
          unclaimedRefunds = unclaimedRefunds.add(networkConfig.getPoolDepositInLovelace());
        }
      }
    }

    return unclaimedRefunds;
  }
}
