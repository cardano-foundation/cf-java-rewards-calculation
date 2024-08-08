package org.cardanofoundation.rewards.validation;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.TreasuryCalculation;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.domain.EpochValidationInput;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.validation.domain.PoolValidationResult;
import org.cardanofoundation.rewards.validation.domain.RewardValidation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toCollection;
import static org.cardanofoundation.rewards.calculation.PoolRewardsCalculation.calculatePoolRewardInEpoch;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.*;

@Slf4j
public class PoolRewardValidation {

    public static PoolRewardCalculationResult computePoolRewardInEpoch(String poolId, int epoch, ProtocolParameters protocolParameters,
                                                                       Epoch epochInfo, BigInteger stakePoolRewardsPot,
                                                                       BigInteger adaInCirculation, PoolState poolStateCurrentEpoch,
                                                                       HashSet<String> accountDeregistrations, HashSet<String> lateAccountDeregistrations,
                                                                       HashSet<String> accountsRegisteredInThePast,
                                                                       HashSet<String> poolIdsWithSharedRewardAddresses,
                                                                       NetworkConfig networkConfig) {
        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210

        if(poolStateCurrentEpoch == null) {
            return PoolRewardCalculationResult.builder().poolId(poolId).epoch(epoch).poolReward(BigInteger.ZERO).build();
        }

        BigInteger activeStakeInEpoch = BigInteger.ZERO;
        if (epochInfo.getActiveStake() != null) {
            activeStakeInEpoch = epochInfo.getActiveStake();
        }

        int totalBlocksInEpoch = epochInfo.getBlockCount();

        BigDecimal decentralizationParameter = protocolParameters.getDecentralisation();
        BigDecimal decentralizationThreshold = BigDecimal.valueOf(0.8);
        if (isLower(decentralizationParameter, decentralizationThreshold) && isHigher(decentralizationParameter, BigDecimal.ZERO)) {
            totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
        }

        // Step 10 a: Check if pool reward address or member stake addresses have been unregistered before
        List<String> stakeAddresses = new ArrayList<>();
        stakeAddresses.add(poolStateCurrentEpoch.getRewardAddress());
        stakeAddresses.addAll(poolStateCurrentEpoch.getDelegators().stream().map(Delegator::getStakeAddress).toList());

        HashSet<String> delegatorDeregistrations = accountDeregistrations.stream()
                .filter(stakeAddresses::contains).collect(toCollection(HashSet::new));

        // There was a different behavior in the previous version of the node
        // If a pool reward address had been used for multiple pools,
        // the stake account only received the reward for one of those pools
        // This is not the case anymore and the stake account receives the reward for all pools
        // Until the Allegra hard fork, this method will be used to emulate the old behavior
        boolean ignoreLeaderReward = false;

        if (epoch < networkConfig.getMainnetAllegraHardforkEpoch()) {
            ignoreLeaderReward = poolIdsWithSharedRewardAddresses.contains(poolId);
        }

        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"
        return calculatePoolRewardInEpoch(poolId, poolStateCurrentEpoch,
                totalBlocksInEpoch, protocolParameters,
                adaInCirculation, activeStakeInEpoch, stakePoolRewardsPot,
                poolStateCurrentEpoch.getOwnerActiveStake(), poolStateCurrentEpoch.getOwners(),
                delegatorDeregistrations, ignoreLeaderReward, lateAccountDeregistrations,
                accountsRegisteredInThePast, networkConfig);

    }

    public static PoolRewardCalculationResult computePoolRewardInEpoch(String poolId, int epoch, DataProvider dataProvider, NetworkConfig networkConfig) {
        // The Shelley era and the ada pot system started on mainnet in epoch 208.
        // Fee and treasury values are 0 for epoch 208.

        PoolRewardCalculationResult poolRewardsCalculationResult = PoolRewardCalculationResult.builder()
                .poolId(poolId).epoch(epoch).poolReward(BigInteger.ZERO).build();
        if (epoch < networkConfig.getMainnetShelleyStartEpoch()) {
            log.warn("Epoch " + epoch + " is before the start of the Shelley era. No rewards were calculated in this epoch.");
            return poolRewardsCalculationResult;
        } else if (epoch == networkConfig.getMainnetShelleyStartEpoch()) {
            return poolRewardsCalculationResult;
        }

        if (dataProvider instanceof JsonDataProvider) {
            EpochValidationInput epochValidationInput = ((JsonDataProvider) dataProvider).getEpochValidationInput(epoch + 2);

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

            // Calculate total available reward for pools (total reward pot after treasury cut)
            BigDecimal decentralizationParameter = protocolParameters.getDecentralisation();
            BigDecimal monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
            BigDecimal treasuryGrowRate = protocolParameters.getTreasuryGrowRate();

            BigInteger adaInCirculation = networkConfig.getTotalLovelace().subtract(epochValidationInput.getReservesOfPreviousEpoch());

            BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
            int totalBlocksInEpoch = 0;

            if (epochInfo != null) {
                totalFeesForCurrentEpoch = epochInfo.getFees();
                totalBlocksInEpoch = epochInfo.getBlockCount();
                if (isLower(decentralizationParameter, BigDecimal.valueOf(0.8)) && isHigher(decentralizationParameter, BigDecimal.ZERO)) {
                    totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
                }
            }

            final int blocksInEpoch = totalBlocksInEpoch;


            BigInteger totalRewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                    monetaryExpandRate, blocksInEpoch, decentralizationParameter, epochValidationInput.getReservesOfPreviousEpoch(), totalFeesForCurrentEpoch, networkConfig);

            BigInteger stakePoolRewardsPot = totalRewardPot.subtract(floor(multiply(totalRewardPot, treasuryGrowRate)));

            poolRewardsCalculationResult = computePoolRewardInEpoch(poolId, epoch, protocolParameters, epochInfo, stakePoolRewardsPot, adaInCirculation,
                    epochValidationInput.getPoolStates().stream().filter(poolState -> poolState.getPoolId().equals(poolId)).findFirst().orElse(null),
                    epochValidationInput.getDeregisteredAccounts(), epochValidationInput.getLateDeregisteredAccounts(),
                    epochValidationInput.getRegisteredAccountsUntilNow(), epochValidationInput.getSharedPoolRewardAddressesWithoutReward(), networkConfig);
        } else {
            // Get Epoch information of current epoch
            // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
            Epoch epochInfo = dataProvider.getEpochInfo(epoch, networkConfig);
            BigInteger totalFeesForCurrentEpoch = epochInfo.getFees();

            int totalBlocksInEpoch = epochInfo.getBlockCount();

            // Get the ada reserves for the next epoch because it was already updated (int the previous step)
            AdaPots adaPotsForNextEpoch = dataProvider.getAdaPotsForEpoch(epoch + 1);
            BigInteger reserves = adaPotsForNextEpoch.getReserves();

            // Get total ada in circulation
            BigInteger adaInCirculation = networkConfig.getTotalLovelace().subtract(reserves);

            // Get protocol parameters for current epoch
            ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch);

            // Calculate total available reward for pools (total reward pot after treasury cut)
            BigDecimal decentralizationParameter = protocolParameters.getDecentralisation();
            BigDecimal monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
            BigDecimal treasuryGrowRate = protocolParameters.getTreasuryGrowRate();

            BigDecimal decentralizationThreshold = BigDecimal.valueOf(0.8);
            if (isLower(decentralizationParameter, decentralizationThreshold) && isHigher(decentralizationParameter, BigDecimal.ZERO)) {
                totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
            }

            BigInteger totalRewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                    monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserves, totalFeesForCurrentEpoch, networkConfig);

            BigInteger stakePoolRewardsPot = totalRewardPot.subtract(floor(multiply(totalRewardPot, treasuryGrowRate)));

            PoolState poolStateCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch);

            HashSet<String> accountDeregistrations;
            HashSet<String> lateAccountDeregistrations = new HashSet<>();
            if (epoch < networkConfig.getMainnetVasilHardforkEpoch()) {
                accountDeregistrations = dataProvider.getDeregisteredAccountsInEpoch(epoch + 1, networkConfig.getRandomnessStabilisationWindow());
                HashSet<String> deregisteredAccountsOnEpochBoundary = dataProvider.getDeregisteredAccountsInEpoch(epoch + 1, networkConfig.getExpectedSlotsPerEpoch());
                lateAccountDeregistrations = deregisteredAccountsOnEpochBoundary.stream().filter(account -> !accountDeregistrations.contains(account)).collect(Collectors.toCollection(HashSet::new));
            } else {
                accountDeregistrations = dataProvider.getDeregisteredAccountsInEpoch(epoch + 1, networkConfig.getExpectedSlotsPerEpoch());
            }

            HashSet<String> sharedPoolRewardAddressesWithoutReward = dataProvider.findSharedPoolRewardAddressWithoutReward(epoch);
            HashSet<String> rewardAddresses = new HashSet<>();
            if (poolStateCurrentEpoch != null) {
                rewardAddresses = new HashSet<>(List.of(poolStateCurrentEpoch.getRewardAddress()));
            }

            long stabilityWindow = networkConfig.getRandomnessStabilisationWindow();
            // Since the Vasil hard fork, the unregistered accounts will not filter out before the
            // rewards calculation starts (at the stability window). They will be filtered out on the
            // epoch boundary when the reward update will be applied.
            if (epoch >= networkConfig.getMainnetVasilHardforkEpoch()) {
                stabilityWindow = networkConfig.getExpectedSlotsPerEpoch();
            }

            HashSet<String> accountsRegisteredInThePast = dataProvider.getRegisteredAccountsUntilLastEpoch(epoch + 2, rewardAddresses, stabilityWindow);
            poolRewardsCalculationResult = computePoolRewardInEpoch(poolId, epoch, protocolParameters, epochInfo, stakePoolRewardsPot, adaInCirculation, poolStateCurrentEpoch,
                    accountDeregistrations, lateAccountDeregistrations, accountsRegisteredInThePast, sharedPoolRewardAddressesWithoutReward, networkConfig);
        }
        return poolRewardsCalculationResult;
    }

    public static PoolValidationResult validatePoolRewardCalculation(PoolRewardCalculationResult poolRewardCalculationResult, DataProvider dataProvider) {
        int epoch  = poolRewardCalculationResult.getEpoch();

        if (dataProvider instanceof JsonDataProvider) {
            epoch += 2;
        }

        HashSet<Reward> memberRewardsInEpoch = dataProvider.getMemberRewardsInEpoch(epoch);
        HashSet<PoolReward> totalPoolRewards = dataProvider.getTotalPoolRewardsInEpoch(epoch);
        return validatePoolRewardCalculation(poolRewardCalculationResult, memberRewardsInEpoch, totalPoolRewards);
    }

    public static PoolValidationResult validatePoolRewardCalculation(PoolRewardCalculationResult poolRewardCalculationResult, HashSet<Reward> memberRewardsInEpoch, HashSet<PoolReward> totalPoolRewards) {
        String poolId = poolRewardCalculationResult.getPoolId();
        PoolValidationResult poolRewardValidationResult = PoolValidationResult.from(poolRewardCalculationResult, new HashSet<>());

        BigInteger actualPoolReward = totalPoolRewards.stream()
                .filter(reward -> reward.getPoolId().equals(poolId))
                .map(PoolReward::getAmount)
                .reduce(BigInteger.ZERO, BigInteger::add);
        HashSet<Reward> actualPoolRewardsInEpoch = memberRewardsInEpoch.stream()
                .filter(reward -> reward.getPoolId().equals(poolId))
                .collect(toCollection(HashSet::new));

        if (actualPoolReward.equals(BigInteger.ZERO)) {
            if (!poolRewardCalculationResult.getPoolReward().equals(BigInteger.ZERO)) {
                log.info("Pool reward is zero for pool " + poolId + " but calculated pool reward is " + poolRewardCalculationResult.getPoolReward().longValue() + " Lovelace");
            }
            return poolRewardValidationResult;
        }

        BigInteger totalDifference = BigInteger.ZERO;
        int rewardIndex = 0;
        HashSet<RewardValidation> rewardValidations = new HashSet<>();
        HashSet<Reward> delegatorStakeAddresses = new HashSet<>(poolRewardCalculationResult.getMemberRewards());
        for (Reward reward : actualPoolRewardsInEpoch) {
            Reward memberReward = delegatorStakeAddresses.stream()
                    .filter(member -> member.getStakeAddress().equals(reward.getStakeAddress()))
                    .findFirst()
                    .orElse(null);

            RewardValidation rewardValidation = RewardValidation.builder()
                    .stakeAddress(reward.getStakeAddress())
                    .expectedReward(reward.getAmount())
                    .calculatedReward(BigInteger.ZERO).build();

            if (memberReward == null) {
                log.info("Member reward not found for stake address: " + reward.getStakeAddress());
                log.info("[" + rewardIndex + "] The expected member " + reward.getStakeAddress() + " reward would be : " + reward.getAmount().longValue() + " Lovelace");
                totalDifference = totalDifference.add(reward.getAmount());
            } else {
                rewardValidation.setCalculatedReward(memberReward.getAmount());
                BigInteger difference = reward.getAmount().subtract(memberReward.getAmount()).abs();

                if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(reward.getStakeAddress())) {
                    BigInteger poolOwnerReward = poolRewardCalculationResult.getOperatorReward();
                    difference = reward.getAmount().subtract(poolOwnerReward).abs();
                }
                totalDifference = totalDifference.add(difference);

                if (difference.compareTo(BigInteger.ZERO) > 0) {
                    log.info("[" + rewardIndex + "] The difference between expected member " + reward.getStakeAddress() + " reward and actual member reward is : " + difference.longValue() + " Lovelace");
                }
            }
            rewardValidations.add(rewardValidation);
            rewardIndex++;
        }

        if (isHigher(totalDifference, BigInteger.ZERO)) {
            log.info("Total difference: " + totalDifference.longValue() + " Lovelace");
        }

        BigInteger totalNoReward = BigInteger.ZERO;
        BigInteger coOwnerReward = BigInteger.ZERO;
        HashSet<Reward> actualRewards = new HashSet<>(actualPoolRewardsInEpoch);

        if (poolRewardCalculationResult.getMemberRewards() != null) {
            for (Reward memberReward : poolRewardCalculationResult.getMemberRewards()) {
                Reward actualReward = actualRewards.stream()
                        .filter(reward -> reward.getStakeAddress().equals(memberReward.getStakeAddress()))
                        .findFirst()
                        .orElse(null);
                if (actualReward == null && isHigher(memberReward.getAmount(), BigInteger.ZERO) && !poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                    totalNoReward = totalNoReward.add(memberReward.getAmount());
                    log.info("No reward! The difference between expected member " + memberReward.getStakeAddress() + " reward and actual member reward is : " + memberReward.getAmount().longValue() + " Lovelace");
                    rewardValidations.add(RewardValidation.builder()
                            .stakeAddress(memberReward.getStakeAddress())
                            .expectedReward(BigInteger.ZERO)
                            .calculatedReward(memberReward.getAmount()).build());
                }

                // Co-owner reward is not included in the member rewards and would be added to the reward address of the pool
                if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                    coOwnerReward = coOwnerReward.add(memberReward.getAmount());
                }
            }

            if (isHigher(totalNoReward, BigInteger.ZERO)) {
                log.info("Total no reward: " + totalNoReward.longValue() + " Lovelace");
            }
        }

        poolRewardValidationResult = PoolValidationResult.from(poolRewardCalculationResult, rewardValidations);

        if (!poolRewardValidationResult.isValid()) {
            log.info("The difference between expected pool reward and actual pool reward is : " + poolRewardValidationResult.getOffset().longValue() + " Lovelace");
        }

        return poolRewardValidationResult;
    }
}
