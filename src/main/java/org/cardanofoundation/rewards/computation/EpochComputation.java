package org.cardanofoundation.rewards.computation;

import org.cardanofoundation.rewards.calculation.PoolRewardsCalculation;
import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.entity.jpa.projection.LatestStakeAccountUpdate;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;
import org.cardanofoundation.rewards.enums.MirPot;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.BigNumberUtils.*;

public class EpochComputation {

    public static EpochCalculationResult calculateEpochPots(int epoch, DataProvider dataProvider) {
        long epochCalculationStart = System.currentTimeMillis();
        EpochCalculationResult epochCalculationResult = EpochCalculationResult.builder().epoch(epoch).build();

        double treasuryGrowthRate = 0.2;
        double monetaryExpandRate = 0.003;
        double decentralizationParameter = 1;

        AdaPots adaPotsForPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1);

        BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
        int totalBlocksInEpoch = 0;

        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch - 2);
        Epoch epochInfo = dataProvider.getEpochInfo(epoch - 2);

        /* We need to use the epoch info 2 epochs before as shelley starts in epoch 208 it will be possible to get
           those values from epoch 210 onwards. Before that we need to use the genesis values, but they are not
           needed anyway if the decentralization parameter is > 0.8.
           See: shelley-delegation.pdf 5.4.3 */
        if (epoch > 209) {
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
        BigInteger stakePoolRewardsPot = rewardPot.subtract(treasuryCut);

        // The sum of all the refunds attached to unregistered reward accounts are added to the
        // treasury (see: Pool Reap Transition, p.53, figure 40, shely-ledger.pdf)
        List<PoolDeregistration> retiredPools = dataProvider.getRetiredPoolsInEpoch(epoch);

        if (retiredPools.size() > 0) {
            // The deposit will pay back one epoch later
            List<AccountUpdate> accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(
                    retiredPools.stream().map(PoolDeregistration::getRewardAddress).toList(), epoch - 1);

            treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(
                    TreasuryComputation.calculateUnclaimedRefundsForRetiredPools(retiredPools, accountUpdates));
        }
        // Check if there was a MIR Certificate in the previous epoch
        BigInteger treasuryWithdrawals = BigInteger.ZERO;
        List<MirCertificate> mirCertificates = dataProvider.getMirCertificatesInEpoch(epoch - 1);
        for (MirCertificate mirCertificate : mirCertificates) {
            if (mirCertificate.getPot() == MirPot.TREASURY) {
                treasuryWithdrawals = treasuryWithdrawals.add(mirCertificate.getTotalRewards());
            }
        }
        treasuryForCurrentEpoch = treasuryForCurrentEpoch.subtract(treasuryWithdrawals);

        List<String> poolIds = dataProvider.getPoolsThatProducedBlocksInEpoch(epoch - 2);
        BigInteger totalDistributedRewards = BigInteger.ZERO;

        BigInteger adaInCirculation = TOTAL_LOVELACE.subtract(reserveInPreviousEpoch);
        List<PoolRewardCalculationResult> PoolRewardCalculationResults = new ArrayList<>();

        int processedPools = 0;
        long start = System.currentTimeMillis();
        List<PoolHistory> poolHistories = dataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2);
        List<String> stakeAddresses = new ArrayList<>(poolHistories.stream()
                .map(history -> history.getDelegators().stream()
                        .map(Delegator::getStakeAddress).toList())
                .flatMap(Collection::stream).toList());
        stakeAddresses.addAll(poolHistories.stream().map(PoolHistory::getRewardAddress).toList());

        List<LatestStakeAccountUpdate> accountUpdates = dataProvider.getLatestStakeAccountUpdates(epoch - 2, stakeAddresses);
        long end = System.currentTimeMillis();
        System.out.println("Pools history fetched in " + (end - start) + "ms");
        for (String poolId : poolIds) {
            start = System.currentTimeMillis();
            System.out.println("[" + processedPools + "/" + poolIds.size() + "] Processing pool: " + poolId);
            PoolHistory poolHistory = poolHistories.stream().filter(history -> history.getPoolId().equals(poolId)).findFirst().orElse(null);
            PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardComputation.computePoolRewardInEpoch(poolId, epoch - 2, protocolParameters, epochInfo, stakePoolRewardsPot, adaInCirculation, poolHistory, accountUpdates);

            /*if (!PoolRewardComputation.poolRewardIsValid(poolRewardCalculationResult, dataProvider)) {
                return epochCalculationResult;
            }*/
            PoolRewardCalculationResults.add(poolRewardCalculationResult);
            totalDistributedRewards = add(totalDistributedRewards, poolRewardCalculationResult.getDistributedPoolReward());
            processedPools++;
            end = System.currentTimeMillis();
            System.out.println("Pool processed in " + (end - start) + "ms");
        }

        BigInteger calculatedReserve = subtract(reserveInPreviousEpoch, subtract(rewardPot, totalFeesForCurrentEpoch));
        BigInteger undistributedRewards = subtract(stakePoolRewardsPot, totalDistributedRewards);
        calculatedReserve = add(calculatedReserve, undistributedRewards);
        long epochCalculationEnd = System.currentTimeMillis();
        System.out.println("Epoch calculation took " +
                Math.round((epochCalculationEnd - epochCalculationStart) / 1000.0)  + " seconds in total.");

        epochCalculationResult.setTotalDistributedRewards(totalDistributedRewards);
        epochCalculationResult.setTotalRewardsPot(rewardPot);
        epochCalculationResult.setReserves(calculatedReserve);
        epochCalculationResult.setTreasury(treasuryForCurrentEpoch);
        epochCalculationResult.setPoolRewardCalculationResults(PoolRewardCalculationResults);
        epochCalculationResult.setTotalPoolRewardsPot(stakePoolRewardsPot);
        epochCalculationResult.setTotalAdaInCirculation(adaInCirculation);
        epochCalculationResult.setTotalUndistributedRewards(undistributedRewards);

        return epochCalculationResult;
    }
}
