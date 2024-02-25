package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.calculation.enums.MirPot;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.cardanofoundation.rewards.calculation.PoolRewardsCalculation.calculatePoolRewardInEpoch;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.POOL_DEPOSIT_IN_LOVELACE;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.*;
import static org.cardanofoundation.rewards.calculation.util.CurrencyConverter.lovelaceToAda;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpochCalculation {

    public static EpochCalculationResult calculateEpochRewardPots(int epoch, AdaPots adaPotsForPreviousEpoch,
                                                                  ProtocolParameters protocolParameters, Epoch epochInfo,
                                                                  List<PoolDeregistration> retiredPools,
                                                                  List<String> deregisteredAccounts,
                                                                  List<MirCertificate> mirCertificates,
                                                                  List<String> poolsThatProducedBlocksInEpoch,
                                                                  List<PoolHistory> poolHistories,
                                                                  List<String> lateDeregisteredAccounts,
                                                                  List<String> accountsRegisteredInThePast,
                                                                  List<String> sharedPoolRewardAddressesWithoutReward) {
        EpochCalculationResult epochCalculationResult = EpochCalculationResult.builder().epoch(epoch).build();

        double treasuryGrowthRate = 0.2;
        double monetaryExpandRate = 0.003;
        double decentralizationParameter = 1;

        BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
        int totalBlocksInEpoch = 0;

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

        BigInteger rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch);

        BigInteger treasuryCut = multiplyAndFloor(rewardPot, treasuryGrowthRate);
        BigInteger treasuryForCurrentEpoch = treasuryInPreviousEpoch.add(treasuryCut);
        BigInteger stakePoolRewardsPot = rewardPot.subtract(treasuryCut);

        // The sum of all the refunds attached to unregistered reward accounts are added to the
        // treasury (see: Pool Reap Transition, p.53, figure 40, shely-ledger.pdf)
        if (retiredPools.size() > 0) {
            List<String> rewardAddressesOfRetiredPools = retiredPools.stream().map(PoolDeregistration::getRewardAddress).toList();
            List<String> deregisteredOwnerAccounts = deregisteredAccounts.stream()
                    .filter(rewardAddressesOfRetiredPools::contains).toList();
            List<String> lateDeregisteredOwnerAccounts = lateDeregisteredAccounts.stream()
                    .filter(rewardAddressesOfRetiredPools::contains).toList();

            if (deregisteredOwnerAccounts.size() > 0 || lateDeregisteredOwnerAccounts.size() > 0) {
                treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(POOL_DEPOSIT_IN_LOVELACE);
            }
        }
        // Check if there was a MIR Certificate in the previous epoch
        BigInteger treasuryWithdrawals = BigInteger.ZERO;
        for (MirCertificate mirCertificate : mirCertificates) {
            if (mirCertificate.getPot() == MirPot.TREASURY) {
                treasuryWithdrawals = treasuryWithdrawals.add(mirCertificate.getTotalRewards());
            }
        }
        treasuryForCurrentEpoch = treasuryForCurrentEpoch.subtract(treasuryWithdrawals);

        BigInteger totalDistributedRewards = BigInteger.ZERO;
        BigInteger adaInCirculation = TOTAL_LOVELACE.subtract(reserveInPreviousEpoch);
        List<PoolRewardCalculationResult> PoolRewardCalculationResults = new ArrayList<>();

        int processedPools = 0;
        BigInteger unspendableEarnedRewards = BigInteger.ZERO;

        for (String poolId : poolsThatProducedBlocksInEpoch) {
            log.info("[" + processedPools + "/" + poolsThatProducedBlocksInEpoch.size() + "] Processing pool: " + poolId);
            PoolHistory poolHistory = poolHistories.stream().filter(history -> history.getPoolId().equals(poolId)).findFirst().orElse(null);

            PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculationResult
                    .builder().poolId(poolId).epoch(epoch).poolReward(BigInteger.ZERO).build();

            if(poolHistory != null) {
                BigInteger activeStakeInEpoch = BigInteger.ZERO;
                if (epochInfo.getActiveStake() != null) {
                    activeStakeInEpoch = epochInfo.getActiveStake();
                }

                if (epoch > 212 && epoch < 255) {
                    totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
                }

                // Step 10 a: Check if pool reward address or member stake addresses have been unregistered before
                List<String> stakeAddresses = new ArrayList<>();
                stakeAddresses.add(poolHistory.getRewardAddress());
                stakeAddresses.addAll(poolHistory.getDelegators().stream().map(Delegator::getStakeAddress).toList());

                List<String> latestAccountDeregistrations = deregisteredAccounts.stream()
                        .filter(stakeAddresses::contains).toList();

                // There was a different behavior in the previous version of the node
                // If a pool reward address had been used for multiple pools,
                // the stake account only received the reward for one of those pools
                // This is not the case anymore and the stake account receives the reward for all pools
                // Until the Allegra hard fork, this method will be used to emulate the old behavior
                boolean ignoreLeaderReward = sharedPoolRewardAddressesWithoutReward.contains(poolId);

                poolRewardCalculationResult = calculatePoolRewardInEpoch(poolId, poolHistory,
                        totalBlocksInEpoch, protocolParameters,
                        adaInCirculation, activeStakeInEpoch, stakePoolRewardsPot,
                        poolHistory.getOwnerActiveStake(), poolHistory.getOwners(),
                        latestAccountDeregistrations, ignoreLeaderReward, lateDeregisteredAccounts, accountsRegisteredInThePast);
            }

            PoolRewardCalculationResults.add(poolRewardCalculationResult);
            totalDistributedRewards = add(totalDistributedRewards, poolRewardCalculationResult.getDistributedPoolReward());
            unspendableEarnedRewards = unspendableEarnedRewards.add(poolRewardCalculationResult.getUnspendableEarnedRewards());
            processedPools++;
        }

        BigInteger calculatedReserve = subtract(reserveInPreviousEpoch, subtract(rewardPot, totalFeesForCurrentEpoch));
        BigInteger undistributedRewards = subtract(stakePoolRewardsPot, totalDistributedRewards);
        calculatedReserve = add(calculatedReserve, undistributedRewards);
        calculatedReserve = subtract(calculatedReserve, unspendableEarnedRewards);

        log.info("Unspendable earned rewards: " + lovelaceToAda(unspendableEarnedRewards.intValue()) + " ADA");
        treasuryForCurrentEpoch = add(treasuryForCurrentEpoch, unspendableEarnedRewards);

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
