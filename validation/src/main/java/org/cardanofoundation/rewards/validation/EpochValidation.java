package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.EpochCalculation;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.domain.*;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpochValidation {

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, DataProvider dataProvider, NetworkConfig networkConfig) {
        return calculateEpochRewardPots(epoch, dataProvider, true, networkConfig);
    }

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, DataProvider dataProvider, boolean detailedValidation, NetworkConfig networkConfig) {
        if (epoch < networkConfig.getShelleyStartEpoch()) {
            log.warn("Epoch " + epoch + " is before the start of the Shelley era. No rewards were calculated in this epoch.");
            return EpochCalculationResult.builder()
                    .totalRewardsPot(BigInteger.ZERO)
                    .treasury(BigInteger.ZERO)
                    .reserves(BigInteger.ZERO)
                    .treasuryCalculationResult(TreasuryCalculationResult.builder()
                            .totalRewardPot(BigInteger.ZERO)
                            .treasury(BigInteger.ZERO)
                            .treasuryWithdrawals(BigInteger.ZERO)
                            .unspendableEarnedRewards(BigInteger.ZERO)
                            .epoch(epoch).build())
                    .totalDistributedRewards(BigInteger.ZERO)
                    .epoch(epoch)
                    .build();
        } else if (epoch == networkConfig.getShelleyStartEpoch()) {
            return EpochCalculationResult.builder()
                    .totalRewardsPot(BigInteger.ZERO)
                    .treasury(BigInteger.ZERO)
                    .treasuryCalculationResult(TreasuryCalculationResult.builder()
                            .totalRewardPot(BigInteger.ZERO)
                            .treasury(BigInteger.ZERO)
                            .treasuryWithdrawals(BigInteger.ZERO)
                            .unspendableEarnedRewards(BigInteger.ZERO)
                            .epoch(epoch).build())
                    .reserves(networkConfig.getShelleyInitialReserves())
                    .totalDistributedRewards(BigInteger.ZERO)
                    .epoch(epoch)
                    .build();
        }

        long overallStart = System.currentTimeMillis();

        EpochCalculationResult epochCalculationResult;
        HashSet<Reward> memberRewardsInEpoch = new HashSet<>();
        HashSet<PoolReward> totalPoolRewards = new HashSet<>();
        if (dataProvider instanceof JsonDataProvider) {
            long start = System.currentTimeMillis();
            log.debug("Start obtaining the epoch data");
            EpochValidationInput epochValidationInput = ((JsonDataProvider) dataProvider).getEpochValidationInput(epoch);

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

            HashSet<String> poolIds = epochValidationInput.getPoolStates().stream().map(PoolState::getPoolId).collect(Collectors.toCollection(HashSet::new));

            if (detailedValidation) {
                memberRewardsInEpoch = epochValidationInput.getPoolRewards().stream()
                        .flatMap(poolReward -> poolReward.getDelegatorRewards().stream()
                        .map(reward -> Reward.builder()
                                .poolId(poolReward.getPoolId())
                                .stakeAddress(reward.getStakeAddress())
                                .amount(reward.getReward())
                                .build()))
                        .collect(Collectors.toCollection(HashSet::new));
                totalPoolRewards = epochValidationInput.getPoolRewards().stream()
                        .map(poolReward -> PoolReward.builder()
                                .poolId(poolReward.getPoolId())
                                .epoch(epoch)
                                .amount(poolReward.getTotalPoolReward())
                                .build())
                        .collect(Collectors.toCollection(HashSet::new));
            }

            epochCalculationResult = EpochCalculation.calculateEpochRewardPots(
                    epoch, epochValidationInput.getReservesOfPreviousEpoch(),
                    epochValidationInput.getTreasuryOfPreviousEpoch(), protocolParameters, epochInfo, epochValidationInput.getRewardAddressesOfRetiredPoolsInEpoch(),
                    epochValidationInput.getDeregisteredAccounts(),
                    new ArrayList<>(epochValidationInput.getMirCertificates()),
                    new ArrayList<>(poolIds),
                    new ArrayList<>(epochValidationInput.getPoolStates()),
                    epochValidationInput.getLateDeregisteredAccounts(),
                    epochValidationInput.getRegisteredAccountsSinceLastEpoch(),
                    epochValidationInput.getRegisteredAccountsUntilNow(), epochValidationInput.getSharedPoolRewardAddressesWithoutReward(),
                    epochValidationInput.getDeregisteredAccountsOnEpochBoundary(), networkConfig);
            long end = System.currentTimeMillis();
            log.debug("Epoch calculation took " + Math.round((end - start) / 1000.0) + "s");
        } else {
            long start = System.currentTimeMillis();
            log.debug("Start obtaining the epoch data");
            AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
            ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
            Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2, networkConfig);
            HashSet<String> rewardAddressesOfRetiredPoolsInEpoch = dataProvider.getRewardAddressesOfRetiredPoolsInEpoch(epoch);
            List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
            List<PoolBlock> blocksMadeByPoolsInEpoch = dataProvider.getBlocksMadeByPoolsInEpoch(epoch - 2);
            List<String> poolIds = blocksMadeByPoolsInEpoch.stream().map(PoolBlock::getPoolId).distinct().toList();
            List<PoolState> poolStates = dataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2, blocksMadeByPoolsInEpoch);

            HashSet<String> deregisteredAccounts;
            HashSet<String> deregisteredAccountsOnEpochBoundary;
            HashSet<String> lateDeregisteredAccounts = new HashSet<>();
            if (epoch - 2 < networkConfig.getVasilHardforkEpoch()) {
                deregisteredAccounts = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, networkConfig.getRandomnessStabilisationWindow());
                deregisteredAccountsOnEpochBoundary = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, networkConfig.getExpectedSlotsPerEpoch());
                lateDeregisteredAccounts = deregisteredAccountsOnEpochBoundary.stream().filter(account -> !deregisteredAccounts.contains(account)).collect(Collectors.toCollection(HashSet::new));
            } else {
                deregisteredAccounts = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, networkConfig.getExpectedSlotsPerEpoch());
                deregisteredAccountsOnEpochBoundary = deregisteredAccounts;
            }

            HashSet<String> sharedPoolRewardAddressesWithoutReward = new HashSet<>();
            if (epoch - 2 < networkConfig.getAllegraHardforkEpoch()) {
                sharedPoolRewardAddressesWithoutReward = dataProvider.findSharedPoolRewardAddressWithoutReward(epoch - 2);
            }
            HashSet<String> poolRewardAddresses = poolStates.stream().map(PoolState::getRewardAddress).collect(Collectors.toCollection(HashSet::new));
            poolRewardAddresses.addAll(rewardAddressesOfRetiredPoolsInEpoch);

            long stabilityWindow = networkConfig.getRandomnessStabilisationWindow();
            // Since the Vasil hard fork, the unregistered accounts will not filter out before the
            // rewards calculation starts (at the stability window). They will be filtered out on the
            // epoch boundary when the reward update will be applied.
            if (epoch - 2 >= networkConfig.getVasilHardforkEpoch()) {
                stabilityWindow = networkConfig.getExpectedSlotsPerEpoch();
            }

            HashSet<String> registeredAccountsSinceLastEpoch = dataProvider.getRegisteredAccountsUntilLastEpoch(epoch, poolRewardAddresses, stabilityWindow);
            HashSet<String> registeredAccountsUntilNow = dataProvider.getRegisteredAccountsUntilNow(epoch, poolRewardAddresses, stabilityWindow);

            if (detailedValidation) {
                memberRewardsInEpoch = dataProvider.getMemberRewardsInEpoch(epoch - 2);
                totalPoolRewards = dataProvider.getTotalPoolRewardsInEpoch(epoch - 2);
            }
            long end = System.currentTimeMillis();
            log.debug("Obtaining the epoch data took " + Math.round((end - start) / 1000.0) + "s");
            log.debug("Start epoch calculation");

            start = System.currentTimeMillis();
            epochCalculationResult = EpochCalculation.calculateEpochRewardPots(
                    epoch, adaPotsForPreviousEpoch.getReserves(), adaPotsForPreviousEpoch.getTreasury(), protocolParameters, epochInfo, rewardAddressesOfRetiredPoolsInEpoch, deregisteredAccounts,
                    mirCertificates, poolIds, poolStates, lateDeregisteredAccounts,
                    registeredAccountsSinceLastEpoch, registeredAccountsUntilNow, sharedPoolRewardAddressesWithoutReward,
                    deregisteredAccountsOnEpochBoundary, networkConfig);
            end = System.currentTimeMillis();
            log.debug("Epoch calculation took " + Math.round((end - start) / 1000.0) + "s");
        }

        if (detailedValidation) {
            log.debug("Start epoch validation");

            long validationStart = System.currentTimeMillis();
            List<PoolValidationResult> poolValidationResults = new ArrayList<>();
            for (PoolRewardCalculationResult poolRewardCalculationResult : epochCalculationResult.getPoolRewardCalculationResults()) {
                long start = System.currentTimeMillis();
                PoolValidationResult poolValidationResult = PoolRewardValidation.validatePoolRewardCalculation(poolRewardCalculationResult, memberRewardsInEpoch, totalPoolRewards);
                poolValidationResults.add(poolValidationResult);

                if (!poolValidationResult.isValid()) {
                    log.info("Pool reward is invalid. Please check the details for pool " + poolRewardCalculationResult.getPoolId());
                }
                long end = System.currentTimeMillis();
                log.debug("Validation of pool " + poolRewardCalculationResult.getPoolId() + " took " + Math.round((end - start) / 1000.0) + "s");
            }
            poolValidationResults.sort(Comparator.comparing(PoolValidationResult::getOffset).reversed());
            if (poolValidationResults.get(0).getOffset().compareTo(BigInteger.ZERO) > 0) {
                log.info("The pool with the largest offset is " + poolValidationResults.get(0).getPoolId() + " with an offset of " + poolValidationResults.get(0).getOffset());
            }
            long validationEnd = System.currentTimeMillis();
            log.debug("Epoch validation took " + Math.round((validationEnd - validationStart) / 1000.0) + "s");
        }

        long overallEnd = System.currentTimeMillis();
        log.info("Overall calculation and validation of epoch " + epoch + " took " +
                Math.round((overallEnd - overallStart) / 1000.0)  + " seconds in total.");

        return epochCalculationResult;
    }
}
