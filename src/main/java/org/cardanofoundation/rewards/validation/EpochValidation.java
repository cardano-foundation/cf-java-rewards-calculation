package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.EpochCalculation;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;
import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpochValidation {

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, DataProvider dataProvider) {
        long overallStart = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        log.info("Start obtaining the epoch data");
        AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);
        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
        Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);
        List<PoolDeregistration> retiredPools = dataProvider.getRetiredPoolsInEpoch(epoch);
        List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
        List<String> poolIds = dataProvider.getPoolsThatProducedBlocksInEpoch(epoch - 2);
        List<PoolHistory> poolHistories = dataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2);
        List<AccountUpdate> accountUpdates = dataProvider.getLatestStakeAccountUpdates(epoch - 1);
        List<Reward> memberRewardsInEpoch = dataProvider.getMemberRewardsInEpoch(epoch - 2);
        List<TotalPoolRewards> totalPoolRewards = dataProvider.getSumOfMemberAndLeaderRewardsInEpoch(epoch - 2);
        List<String> sharedPoolRewardAddressesWithoutReward = dataProvider.findSharedPoolRewardAddressWithoutReward(epoch - 2);
        long end = System.currentTimeMillis();
        log.info("Obtaining the epoch data took " + Math.round((end - start) / 1000.0) + "s");
        log.info("Start epoch calculation");

        start = System.currentTimeMillis();
        EpochCalculationResult epochCalculationResult = EpochCalculation.calculateEpochRewardPots(
                epoch, adaPotsForPreviousEpoch, protocolParameters, epochInfo, retiredPools, accountUpdates,
                mirCertificates, poolIds, poolHistories, sharedPoolRewardAddressesWithoutReward);
        end = System.currentTimeMillis();
        log.info("Epoch calculation took " + Math.round((end - start) / 1000.0) + "s");
        log.info("Start epoch validation");

        start = System.currentTimeMillis();
        for (PoolRewardCalculationResult poolRewardCalculationResult : epochCalculationResult.getPoolRewardCalculationResults()) {
            if (!PoolRewardValidation.poolRewardIsValid(poolRewardCalculationResult, memberRewardsInEpoch, totalPoolRewards)) {
                log.info("Pool reward is invalid. Please check the details for pool " + poolRewardCalculationResult.getPoolId());
            }
        }
        end = System.currentTimeMillis();
        log.info("Epoch validation took " + Math.round((end - start) / 1000.0) + "s");

        long overallEnd = System.currentTimeMillis();
        log.info("Overall calculation and validation took " +
                Math.round((overallEnd - overallStart) / 1000.0)  + " seconds in total.");

        return epochCalculationResult;
    }
}
