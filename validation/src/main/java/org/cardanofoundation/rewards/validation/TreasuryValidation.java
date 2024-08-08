package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.TreasuryCalculation;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.domain.EpochValidationInput;
import org.cardanofoundation.rewards.validation.domain.TreasuryValidationResult;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class TreasuryValidation {

  public static TreasuryValidationResult calculateTreasuryForEpoch(int epoch, DataProvider dataProvider, NetworkConfig networkConfig) {
    TreasuryCalculationResult treasuryCalculationResult;

    if (dataProvider instanceof JsonDataProvider) {
      EpochValidationInput epochValidationInput = ((JsonDataProvider) dataProvider).getEpochValidationInput(epoch);

      AdaPots adaPotsForPreviousEpoch = AdaPots.builder()
              .treasury(epochValidationInput.getTreasuryOfPreviousEpoch())
              .reserves(epochValidationInput.getReservesOfPreviousEpoch())
              .rewards(BigInteger.ZERO)
              .adaInCirculation(BigInteger.ZERO)
              .epoch(epoch - 1)
              .build();

      ProtocolParameters protocolParameters = ProtocolParameters.builder()
              .decentralisation(epochValidationInput.getDecentralisation())
              .monetaryExpandRate(epochValidationInput.getMonetaryExpandRate())
              .treasuryGrowRate(epochValidationInput.getTreasuryGrowRate())
              .optimalPoolCount(epochValidationInput.getOptimalPoolCount())
              .poolOwnerInfluence(epochValidationInput.getPoolOwnerInfluence())
              .build();

      Epoch epochInfo = null;

      if (epochValidationInput.getBlockCount() > 0) {
        epochInfo = Epoch.builder()
                .number(epoch)
                .blockCount(epochValidationInput.getBlockCount())
                .fees(epochValidationInput.getFees())
                .activeStake(epochValidationInput.getActiveStake())
                .nonOBFTBlockCount(epochValidationInput.getNonOBFTBlockCount())
                .build();
      }

      treasuryCalculationResult = TreasuryCalculation.calculateTreasuryInEpoch(epoch, protocolParameters, adaPotsForPreviousEpoch, epochInfo,
              epochValidationInput.getRewardAddressesOfRetiredPoolsInEpoch(),
              epochValidationInput.getMirCertificates().stream().toList(),
              epochValidationInput.getDeregisteredAccountsOnEpochBoundary(),
              epochValidationInput.getRegisteredAccountsUntilNow(), BigInteger.ZERO, networkConfig);
    } else {
      AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
      ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
      Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2, networkConfig);
      HashSet<String> rewardAddressesOfRetiredPoolsInEpoch = dataProvider.getRewardAddressesOfRetiredPoolsInEpoch(epoch);
      List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
      List<PoolBlock> blocksMadeByPoolsInEpoch = dataProvider.getBlocksMadeByPoolsInEpoch(epoch - 2);
      List<PoolState> poolHistories = dataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2, blocksMadeByPoolsInEpoch);
      HashSet<String> deregisteredAccountsOnEpochBoundary = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, networkConfig.getExpectedSlotsPerEpoch());

      HashSet<String> poolRewardAddresses = poolHistories.stream().map(PoolState::getRewardAddress).collect(Collectors.toCollection(HashSet::new));
      poolRewardAddresses.addAll(rewardAddressesOfRetiredPoolsInEpoch);
      HashSet<String> registeredAccountsUntilNow = dataProvider.getRegisteredAccountsUntilNow(epoch, poolRewardAddresses, networkConfig.getRandomnessStabilisationWindow());
      treasuryCalculationResult = TreasuryCalculation.calculateTreasuryInEpoch(epoch, protocolParameters, adaPotsForPreviousEpoch, epochInfo, rewardAddressesOfRetiredPoolsInEpoch, mirCertificates, deregisteredAccountsOnEpochBoundary, registeredAccountsUntilNow, BigInteger.ZERO, networkConfig);
    }

    TreasuryValidationResult treasuryValidationResult = TreasuryValidationResult.fromTreasuryCalculationResult(treasuryCalculationResult);
    AdaPots adaPotsForCurrentEpoch = dataProvider.getAdaPotsForEpoch(epoch);
    treasuryValidationResult.setActualTreasury(adaPotsForCurrentEpoch.getTreasury());

    return treasuryValidationResult;
  }
}
