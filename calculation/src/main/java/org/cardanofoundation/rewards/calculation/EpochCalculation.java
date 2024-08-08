package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.calculation.enums.MirPot;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.cardanofoundation.rewards.calculation.PoolRewardsCalculation.calculatePoolRewardInEpoch;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EpochCalculation {

    public static EpochCalculationResult calculateEpochRewardPots(final int epoch,
                                                                  final BigInteger reserveInPreviousEpoch,
                                                                  final BigInteger treasuryInPreviousEpoch,
                                                                  final ProtocolParameters protocolParameters, final Epoch epochInfo,
                                                                  final HashSet<String> rewardAddressesOfRetiredPools,
                                                                  final HashSet<String> deregisteredAccounts,
                                                                  final List<MirCertificate> mirCertificates,
                                                                  final List<String> poolsThatProducedBlocksInEpoch,
                                                                  final List<PoolState> poolHistories,
                                                                  final HashSet<String> lateDeregisteredAccounts,
                                                                  final HashSet<String> registeredAccountsSinceLastEpoch,
                                                                  final HashSet<String> registeredAccountsUntilNow,
                                                                  final HashSet<String> sharedPoolRewardAddressesWithoutReward,
                                                                  final HashSet<String> deregisteredAccountsOnEpochBoundary,
                                                                  final NetworkConfig networkConfig) {
        final EpochCalculationResult epochCalculationResult = EpochCalculationResult.builder().epoch(epoch).build();

        if (epoch < networkConfig.getMainnetShelleyStartEpoch()) {
            log.warn("Epoch " + epoch + " is before the start of the Shelley era. No rewards were calculated in this epoch.");
            epochCalculationResult.setReserves(BigInteger.ZERO);
            epochCalculationResult.setTreasury(BigInteger.ZERO);
            epochCalculationResult.setTotalDistributedRewards(BigInteger.ZERO);
            epochCalculationResult.setTotalRewardsPot(BigInteger.ZERO);
            epochCalculationResult.setTotalPoolRewardsPot(BigInteger.ZERO);
            epochCalculationResult.setTotalAdaInCirculation(BigInteger.ZERO);
            return epochCalculationResult;
        } else if (epoch == networkConfig.getMainnetShelleyStartEpoch()) {
            epochCalculationResult.setReserves(networkConfig.getMainnetShelleyInitialReserves());
            epochCalculationResult.setTreasury(networkConfig.getMainnetShelleyInitialTreasury());
            epochCalculationResult.setTotalDistributedRewards(BigInteger.ZERO);
            epochCalculationResult.setTotalRewardsPot(BigInteger.ZERO);
            epochCalculationResult.setTotalPoolRewardsPot(BigInteger.ZERO);
            epochCalculationResult.setTotalAdaInCirculation(networkConfig.getMainnetShelleyInitialUtxo());
            return epochCalculationResult;
        }

        BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
        int totalBlocksInEpoch = 0;

        BigDecimal treasuryGrowthRate = protocolParameters.getTreasuryGrowRate();
        BigDecimal monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
        BigDecimal decentralizationParameter = protocolParameters.getDecentralisation();
        BigInteger activeStakeInEpoch = BigInteger.ZERO;

        if (epochInfo != null) {
            activeStakeInEpoch = epochInfo.getActiveStake();
            totalFeesForCurrentEpoch = epochInfo.getFees();
            totalBlocksInEpoch = epochInfo.getBlockCount();
            if (isLower(decentralizationParameter, BigDecimal.valueOf(0.8)) && isHigher(decentralizationParameter, BigDecimal.ZERO)) {
                totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
            }
        }

        final int blocksInEpoch = totalBlocksInEpoch;
        final BigInteger rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserveInPreviousEpoch, totalFeesForCurrentEpoch, networkConfig);

        final BigInteger treasuryCut = multiplyAndFloor(rewardPot, treasuryGrowthRate);
        BigInteger treasuryForCurrentEpoch = treasuryInPreviousEpoch.add(treasuryCut);
        final BigInteger stakePoolRewardsPot = rewardPot.subtract(treasuryCut);

        // The sum of all the refunds attached to unregistered reward accounts are added to the
        // treasury (see: Pool Reap Transition, p.53, figure 40, shelley-ledger.pdf)
        BigInteger unclaimedRefunds = BigInteger.ZERO;
        if (rewardAddressesOfRetiredPools.size() > 0) {
            List<String> deregisteredOwnerAccounts = deregisteredAccountsOnEpochBoundary.stream()
                    .filter(rewardAddressesOfRetiredPools::contains).toList();
            List<String> ownerAccountsRegisteredInThePast = registeredAccountsUntilNow.stream()
                    .filter(rewardAddressesOfRetiredPools::contains).toList();

            /* Check if the reward address of the retired pool has been unregistered before
               or if the reward address has been unregistered after the randomness stabilization window
               or if the reward address has not been registered at all */
            for (String rewardAddress : rewardAddressesOfRetiredPools) {
                if (deregisteredOwnerAccounts.contains(rewardAddress) ||
                        !ownerAccountsRegisteredInThePast.contains(rewardAddress)) {
                    // If the reward address has been unregistered, the deposit can not be returned
                    // and will be added to the treasury instead (Pool Reap see: shelley-ledger.pdf p.53)
                    treasuryForCurrentEpoch = treasuryForCurrentEpoch.add(networkConfig.getPoolDepositInLovelace());
                    unclaimedRefunds = unclaimedRefunds.add(networkConfig.getPoolDepositInLovelace());
                }
            }
        }
        // Check if there was a MIR Certificate in the previous epoch
        BigInteger treasuryWithdrawals = BigInteger.ZERO;
        BigInteger calculatedReserve = subtract(reserveInPreviousEpoch, subtract(rewardPot, totalFeesForCurrentEpoch));

        for (MirCertificate mirCertificate : mirCertificates) {
            if (mirCertificate.getPot() == MirPot.TREASURY) {
                treasuryWithdrawals = treasuryWithdrawals.add(mirCertificate.getTotalRewards());
            } else if (mirCertificate.getPot() == MirPot.RESERVES) {
                calculatedReserve = calculatedReserve.subtract(mirCertificate.getTotalRewards());
            }
        }

        treasuryForCurrentEpoch = treasuryForCurrentEpoch.subtract(treasuryWithdrawals);
        BigInteger totalDistributedRewards = BigInteger.ZERO;
        final BigInteger adaInCirculation = networkConfig.getTotalLovelace().subtract(reserveInPreviousEpoch);
        final List<PoolRewardCalculationResult> poolRewardCalculationResults = new ArrayList<>();
        BigInteger unspendableEarnedRewards = BigInteger.ZERO;

        int i = 1;
        for (String poolId : poolsThatProducedBlocksInEpoch) {
            log.debug("[" + i + " / " + poolsThatProducedBlocksInEpoch.size() + "] Processing pool: " + poolId);
            PoolState poolState = poolHistories.stream().filter(history -> history.getPoolId().equals(poolId)).findFirst().orElse(null);
            PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculationResult
                    .builder().poolId(poolId).epoch(epoch).poolReward(BigInteger.ZERO).build();

            if(poolState != null) {
                // Get the reward addresses of the pool and the reward addresses of its delegators
                final HashSet<String> stakeAddresses = new HashSet<>();
                stakeAddresses.add(poolState.getRewardAddress());
                stakeAddresses.addAll(poolState.getDelegators().stream().map(Delegator::getStakeAddress).toList());
                // We need the get the registration state of those accounts. If they were unregistered before
                // the randomness stabilization window, they will not receive any rewards. The remaining of the
                // reward pot will go back to the reserves
                final HashSet<String> delegatorAccountDeregistrations = deregisteredAccounts.stream()
                        .filter(stakeAddresses::contains).collect(Collectors.toCollection(HashSet::new));
                // If they were unregistered after the randomness stabilization window, rewards will be calculated
                // but they will not be spendable and will be added to the treasury instead
                final HashSet<String> lateDeregisteredDelegators = lateDeregisteredAccounts.stream()
                        .filter(stakeAddresses::contains).collect(Collectors.toCollection(HashSet::new));

                // There was a different behavior in the previous version of the node
                // If a pool reward address had been used for multiple pools,
                // the stake account only received the reward for one of those pools
                // This is not the case anymore and the stake account receives the reward for all pools
                // Until the Allegra hard fork, this method will be used to emulate the old behavior
                boolean ignoreLeaderReward = false;
                if (epoch - 2 < networkConfig.getMainnetAllegraHardforkEpoch()) {
                    ignoreLeaderReward = sharedPoolRewardAddressesWithoutReward.contains(poolId);
                }

                poolRewardCalculationResult = calculatePoolRewardInEpoch(poolId, poolState,
                        blocksInEpoch, protocolParameters,
                        adaInCirculation, activeStakeInEpoch, stakePoolRewardsPot,
                        poolState.getOwnerActiveStake(), poolState.getOwners(),
                        delegatorAccountDeregistrations, ignoreLeaderReward, lateDeregisteredDelegators, registeredAccountsSinceLastEpoch, networkConfig);
            }

            totalDistributedRewards = add(totalDistributedRewards, poolRewardCalculationResult.getDistributedPoolReward());
            unspendableEarnedRewards = unspendableEarnedRewards.add(poolRewardCalculationResult.getUnspendableEarnedRewards());
            poolRewardCalculationResults.add(poolRewardCalculationResult);
            i++;
        }

        BigInteger undistributedRewards = subtract(stakePoolRewardsPot, totalDistributedRewards);
        calculatedReserve = add(calculatedReserve, undistributedRewards);
        calculatedReserve = subtract(calculatedReserve, unspendableEarnedRewards);

        if (epoch == networkConfig.getMainnetAllegraHardforkEpoch()) {
            /*
                "The bootstrap addresses from Figure 6 were not intended to include the Byron era redeem
                addresses (those with addrtype 2, see the Byron CDDL spec). These addresses were, however,
                not spendable in the Shelley era. At the Allegra hard fork they were removed from the UTxO
                and the Ada contained in them was returned to the reserves."
                    - shelley-spec-ledger.pdf 17.5 p.115
             */
            calculatedReserve = calculatedReserve.add(networkConfig.getMainnetBootstrapAddressAmount());
        }

        log.debug("Unspendable earned rewards: " + unspendableEarnedRewards.longValue() + " Lovelace");
        treasuryForCurrentEpoch = add(treasuryForCurrentEpoch, unspendableEarnedRewards);

        TreasuryCalculationResult treasuryCalculationResult = TreasuryCalculationResult.builder()
                .epoch(epoch)
                .treasury(treasuryForCurrentEpoch)
                .totalRewardPot(rewardPot)
                .treasuryWithdrawals(treasuryWithdrawals)
                .unspendableEarnedRewards(unspendableEarnedRewards)
                .unclaimedRefunds(unclaimedRefunds)
                .build();

        epochCalculationResult.setTotalDistributedRewards(totalDistributedRewards);
        epochCalculationResult.setTotalRewardsPot(rewardPot);
        epochCalculationResult.setReserves(calculatedReserve);
        epochCalculationResult.setTreasury(treasuryForCurrentEpoch);
        epochCalculationResult.setPoolRewardCalculationResults(poolRewardCalculationResults);
        epochCalculationResult.setTotalPoolRewardsPot(stakePoolRewardsPot);
        epochCalculationResult.setTotalAdaInCirculation(adaInCirculation);
        epochCalculationResult.setTotalUndistributedRewards(undistributedRewards);
        epochCalculationResult.setTreasuryCalculationResult(treasuryCalculationResult);

        return epochCalculationResult;
    }
}
