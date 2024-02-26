package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.EpochCalculation;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.MAINNET_ALLEGRA_HARDFORK_EPOCH;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.RANDOMNESS_STABILISATION_WINDOW;

@Slf4j
public class EpochValidation {

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, DataProvider dataProvider) {
        return calculateEpochRewardPots(epoch, dataProvider, true);
    }

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, DataProvider dataProvider, boolean detailedValidation) {
        long overallStart = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        log.info("Start obtaining the epoch data");
        AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
        Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);
        List<PoolDeregistration> retiredPools = dataProvider.getRetiredPoolsInEpoch(epoch);
        List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
        List<PoolBlock> blocksMadeByPoolsInEpoch = dataProvider.getBlocksMadeByPoolsInEpoch(epoch - 2);
        List<String> poolIds = blocksMadeByPoolsInEpoch.stream().map(PoolBlock::getPoolId).distinct().toList();
        List<PoolHistory> poolHistories = dataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2, blocksMadeByPoolsInEpoch);
        List<String> deregisteredAccounts = dataProvider.getDeregisteredAccountsInEpoch(epoch - 1, RANDOMNESS_STABILISATION_WINDOW);
        List<String> lateDeregisteredAccounts = dataProvider.getLateAccountDeregistrationsInEpoch(epoch - 1, RANDOMNESS_STABILISATION_WINDOW);
        List<String> sharedPoolRewardAddressesWithoutReward = List.of();
        if (epoch < MAINNET_ALLEGRA_HARDFORK_EPOCH) {
            sharedPoolRewardAddressesWithoutReward = dataProvider.findSharedPoolRewardAddressWithoutReward(epoch - 2);
        }
        List<String> poolRewardAddresses = poolHistories.stream().map(PoolHistory::getRewardAddress).toList();
        List<String> accountsRegisteredInThePast = dataProvider.getStakeAddressesWithRegistrationsUntilEpoch(epoch - 1, poolRewardAddresses, RANDOMNESS_STABILISATION_WINDOW);

        List<Reward> memberRewardsInEpoch = List.of();
        List<TotalPoolRewards> totalPoolRewards = List.of();
        if (detailedValidation) {
            memberRewardsInEpoch = dataProvider.getMemberRewardsInEpoch(epoch - 2);
            totalPoolRewards = dataProvider.getSumOfMemberAndLeaderRewardsInEpoch(epoch - 2);
        }
        long end = System.currentTimeMillis();
        log.info("Obtaining the epoch data took " + Math.round((end - start) / 1000.0) + "s");
        log.info("Start epoch calculation");

        start = System.currentTimeMillis();
        EpochCalculationResult epochCalculationResult = EpochCalculation.calculateEpochRewardPots(
                epoch, adaPotsForPreviousEpoch, protocolParameters, epochInfo, retiredPools, deregisteredAccounts,
                mirCertificates, poolIds, poolHistories, lateDeregisteredAccounts,
                accountsRegisteredInThePast, sharedPoolRewardAddressesWithoutReward);
        end = System.currentTimeMillis();
        log.info("Epoch calculation took " + Math.round((end - start) / 1000.0) + "s");

        if (detailedValidation) {
            log.info("Start epoch validation");

            start = System.currentTimeMillis();
            for (PoolRewardCalculationResult poolRewardCalculationResult : epochCalculationResult.getPoolRewardCalculationResults()) {
                if (!PoolRewardValidation.poolRewardIsValid(poolRewardCalculationResult, memberRewardsInEpoch, totalPoolRewards)) {
                    log.info("Pool reward is invalid. Please check the details for pool " + poolRewardCalculationResult.getPoolId());
                }
            }
            end = System.currentTimeMillis();
            log.info("Epoch validation took " + Math.round((end - start) / 1000.0) + "s");
        }

        long overallEnd = System.currentTimeMillis();
        log.info("Overall calculation and validation took " +
                Math.round((overallEnd - overallStart) / 1000.0)  + " seconds in total.");

        return epochCalculationResult;
    }
}
