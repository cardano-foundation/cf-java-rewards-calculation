package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.EpochCalculation;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.domain.PoolReward;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.validation.domain.PoolValidationResult;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;

@Slf4j
public class EpochValidation {

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, DataProvider dataProvider) {
        return calculateEpochRewardPots(epoch, dataProvider, true);
    }

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, DataProvider dataProvider, boolean detailedValidation) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) {
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
        } else if (epoch == MAINNET_SHELLEY_START_EPOCH) {
            return EpochCalculationResult.builder()
                    .totalRewardsPot(BigInteger.ZERO)
                    .treasury(BigInteger.ZERO)
                    .treasuryCalculationResult(TreasuryCalculationResult.builder()
                            .totalRewardPot(BigInteger.ZERO)
                            .treasury(BigInteger.ZERO)
                            .treasuryWithdrawals(BigInteger.ZERO)
                            .unspendableEarnedRewards(BigInteger.ZERO)
                            .epoch(epoch).build())
                    .reserves(MAINNET_SHELLEY_INITIAL_RESERVES)
                    .totalDistributedRewards(BigInteger.ZERO)
                    .epoch(epoch)
                    .build();
        }

        long overallStart = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        log.debug("Start obtaining the epoch data");
        AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
        Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);
        List<PoolDeregistration> retiredPools = dataProvider.getRetiredPoolsInEpoch(epoch);
        List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
        List<PoolBlock> blocksMadeByPoolsInEpoch = dataProvider.getBlocksMadeByPoolsInEpoch(epoch - 2);
        List<String> poolIds = blocksMadeByPoolsInEpoch.stream().map(PoolBlock::getPoolId).distinct().toList();
        List<PoolHistory> poolHistories = dataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2, blocksMadeByPoolsInEpoch);

        HashSet<String> deregisteredAccounts;
        HashSet<String> lateDeregisteredAccounts = new HashSet<>();
        if (epoch - 2 < MAINNET_VASIL_HARDFORK_EPOCH) {
            deregisteredAccounts = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, RANDOMNESS_STABILISATION_WINDOW);
            lateDeregisteredAccounts = dataProvider.getLateAccountDeregistrationsInEpoch(epoch - 1, RANDOMNESS_STABILISATION_WINDOW);
        } else {
            deregisteredAccounts = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, EXPECTED_SLOTS_PER_EPOCH);
        }

        HashSet<String> sharedPoolRewardAddressesWithoutReward = new HashSet<>();
        if (epoch - 2 < MAINNET_ALLEGRA_HARDFORK_EPOCH) {
            sharedPoolRewardAddressesWithoutReward = dataProvider.findSharedPoolRewardAddressWithoutReward(epoch - 2);
        }
        HashSet<String> poolRewardAddresses = poolHistories.stream().map(PoolHistory::getRewardAddress).collect(Collectors.toCollection(HashSet::new));
        poolRewardAddresses.addAll(retiredPools.stream().map(PoolDeregistration::getRewardAddress).collect(Collectors.toSet()));
        HashSet<String> registeredAccountsSinceLastEpoch = dataProvider.getRegisteredAccountsUntilLastEpoch(epoch, poolRewardAddresses, RANDOMNESS_STABILISATION_WINDOW);
        HashSet<String> registeredAccountsUntilNow = dataProvider.getRegisteredAccountsUntilNow(epoch, poolRewardAddresses, RANDOMNESS_STABILISATION_WINDOW);

        HashSet<Reward> memberRewardsInEpoch = new HashSet<>();
        HashSet<PoolReward> totalPoolRewards = new HashSet<>();
        if (detailedValidation) {
            memberRewardsInEpoch = dataProvider.getMemberRewardsInEpoch(epoch - 2);
            totalPoolRewards = dataProvider.getTotalPoolRewardsInEpoch(epoch - 2);
        }
        long end = System.currentTimeMillis();
        log.debug("Obtaining the epoch data took " + Math.round((end - start) / 1000.0) + "s");
        log.debug("Start epoch calculation");

        start = System.currentTimeMillis();
        EpochCalculationResult epochCalculationResult = EpochCalculation.calculateEpochRewardPots(
                epoch, adaPotsForPreviousEpoch, protocolParameters, epochInfo, retiredPools, deregisteredAccounts,
                mirCertificates, poolIds, poolHistories, lateDeregisteredAccounts,
                registeredAccountsSinceLastEpoch, registeredAccountsUntilNow, sharedPoolRewardAddressesWithoutReward);
        end = System.currentTimeMillis();
        log.debug("Epoch calculation took " + Math.round((end - start) / 1000.0) + "s");

        if (detailedValidation) {
            log.debug("Start epoch validation");

            long validationStart = System.currentTimeMillis();
            List<PoolValidationResult> poolValidationResults = new ArrayList<>();
            for (PoolRewardCalculationResult poolRewardCalculationResult : epochCalculationResult.getPoolRewardCalculationResults()) {
                start = System.currentTimeMillis();
                PoolValidationResult poolValidationResult = PoolRewardValidation.validatePoolRewardCalculation(poolRewardCalculationResult, memberRewardsInEpoch, totalPoolRewards);
                poolValidationResults.add(poolValidationResult);

                if (!poolValidationResult.isValid()) {
                    log.info("Pool reward is invalid. Please check the details for pool " + poolRewardCalculationResult.getPoolId());
                }
                end = System.currentTimeMillis();
                log.debug("Validation of pool " + poolRewardCalculationResult.getPoolId() + " took " + Math.round((end - start) / 1000.0) + "s");
            }
            poolValidationResults.sort(Comparator.comparing(PoolValidationResult::getOffset).reversed());
            log.info("The pool with the largest offset is " + poolValidationResults.get(0).getPoolId() + " with an offset of " + poolValidationResults.get(0).getOffset());
            long validationEnd = System.currentTimeMillis();
            log.debug("Epoch validation took " + Math.round((validationEnd - validationStart) / 1000.0) + "s");
        }

        long overallEnd = System.currentTimeMillis();
        log.info("Overall calculation and validation of epoch " + epoch + " took " +
                Math.round((overallEnd - overallStart) / 1000.0)  + " seconds in total.");

        return epochCalculationResult;
    }
}
