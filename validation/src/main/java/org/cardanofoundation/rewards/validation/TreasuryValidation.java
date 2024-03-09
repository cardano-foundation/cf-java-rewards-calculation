package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.TreasuryCalculation;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.domain.TreasuryValidationResult;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;

public class TreasuryValidation {

  public static TreasuryValidationResult calculateTreasuryForEpoch(int epoch, DataProvider dataProvider) {
    AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
    ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
    Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);
    HashSet<String> rewardAddressesOfRetiredPoolsInEpoch = dataProvider.getRewardAddressesOfRetiredPoolsInEpoch(epoch);
    List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
    List<PoolBlock> blocksMadeByPoolsInEpoch = dataProvider.getBlocksMadeByPoolsInEpoch(epoch - 2);
    List<PoolHistory> poolHistories = dataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2, blocksMadeByPoolsInEpoch);
    HashSet<String> deregisteredAccountsOnEpochBoundary = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, EXPECTED_SLOTS_PER_EPOCH);

    HashSet<String> poolRewardAddresses = poolHistories.stream().map(PoolHistory::getRewardAddress).collect(Collectors.toCollection(HashSet::new));
    poolRewardAddresses.addAll(rewardAddressesOfRetiredPoolsInEpoch);
    HashSet<String> registeredAccountsUntilNow = dataProvider.getRegisteredAccountsUntilNow(epoch, poolRewardAddresses, RANDOMNESS_STABILISATION_WINDOW);

    TreasuryCalculationResult treasuryCalculationResult = TreasuryCalculation.calculateTreasuryInEpoch(epoch, protocolParameters, adaPotsForPreviousEpoch, epochInfo, rewardAddressesOfRetiredPoolsInEpoch, mirCertificates, deregisteredAccountsOnEpochBoundary, registeredAccountsUntilNow, BigInteger.ZERO);

    TreasuryValidationResult treasuryValidationResult = TreasuryValidationResult.fromTreasuryCalculationResult(treasuryCalculationResult);
    AdaPots adaPotsForCurrentEpoch = dataProvider.getAdaPotsForEpoch(epoch);
    treasuryValidationResult.setActualTreasury(adaPotsForCurrentEpoch.getTreasury());
    return treasuryValidationResult;
  }
}
