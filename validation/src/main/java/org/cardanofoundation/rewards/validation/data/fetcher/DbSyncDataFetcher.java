package org.cardanofoundation.rewards.validation.data.fetcher;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.domain.EpochValidationDelegatorReward;
import org.cardanofoundation.rewards.validation.domain.EpochValidationInput;
import org.cardanofoundation.rewards.validation.domain.EpochValidationPoolReward;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.validation.util.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DbSyncDataFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DbSyncDataFetcher.class);
    @Autowired(required = false)
    private DbSyncDataProvider dbSyncDataProvider;

    @Autowired(required = false)
    private JsonDataProvider jsonDataProvider;

    @Value("${spring.profiles.active:Unknown}")
    private String activeProfiles;

    @Value("${json.data-provider.source}")
    private String sourceFolder;

    @Override
    public void fetch(int epoch, boolean override, boolean skipValidationData, NetworkConfig networkConfig) {
        String filePath = String.format("%s/epoch-validation-input-%d.json.gz", sourceFolder, epoch);
        File outputFile = new File(filePath);

        if (epoch <= networkConfig.getShelleyStartEpoch()) {
            logger.info("Skip to fetch epoch validation input data for epoch " + epoch + " because the epoch is before the start of the Shelley era");
            return;
        }

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch epoch validation input data for epoch " + epoch + " because the json file already exists");
            return;
        }

        AdaPots adaPotsForPreviousEpoch = dbSyncDataProvider.getAdaPotsForEpoch(epoch - 1);
        BigInteger treasuryOfPreviousEpoch = adaPotsForPreviousEpoch.getTreasury();
        BigInteger reservesOfPreviousEpoch = adaPotsForPreviousEpoch.getReserves();

        ProtocolParameters protocolParameters = dbSyncDataProvider.getProtocolParametersForEpoch(epoch - 2);
        BigDecimal decentralisation = protocolParameters.getDecentralisation();
        BigDecimal treasuryGrowRate = protocolParameters.getTreasuryGrowRate();
        BigDecimal monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
        Integer optimalPoolCount = protocolParameters.getOptimalPoolCount();
        BigDecimal poolOwnerInfluence = protocolParameters.getPoolOwnerInfluence();

        Epoch epochInfo = dbSyncDataProvider.getEpochInfo(epoch - 2, networkConfig);
        BigInteger fees = BigInteger.ZERO;
        int blockCount = 0;
        BigInteger activeStake = BigInteger.ZERO;
        int nonOBFTBlockCount = 0;

        if (epochInfo != null) {
            activeStake = epochInfo.getActiveStake();
            fees = epochInfo.getFees();
            blockCount = epochInfo.getBlockCount();
            nonOBFTBlockCount = epochInfo.getNonOBFTBlockCount();
        }

        List<MirCertificate> mirCertificates = dbSyncDataProvider.getMirCertificatesInEpoch(epoch - 1);
        List<PoolBlock> blocksMadeByPoolsInEpoch = dbSyncDataProvider.getBlocksMadeByPoolsInEpoch(epoch - 2);
        List<PoolState> poolStates = dbSyncDataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2, blocksMadeByPoolsInEpoch);

        HashSet<String> deregisteredAccounts;
        HashSet<String> deregisteredAccountsOnEpochBoundary;
        HashSet<String> lateDeregisteredAccounts = new HashSet<>();
        HashSet<String> rewardAddressesOfRetiredPoolsInEpoch = dbSyncDataProvider.getRewardAddressesOfRetiredPoolsInEpoch(epoch);
        if (epoch - 2 < networkConfig.getVasilHardforkEpoch()) {
            deregisteredAccounts = dbSyncDataProvider.getDeregisteredAccountsInEpoch(epoch - 1, networkConfig.getRandomnessStabilisationWindow());
            deregisteredAccountsOnEpochBoundary = dbSyncDataProvider.getDeregisteredAccountsInEpoch(epoch - 1, networkConfig.getExpectedSlotsPerEpoch());
            lateDeregisteredAccounts = deregisteredAccountsOnEpochBoundary.stream().filter(account -> !deregisteredAccounts.contains(account)).collect(Collectors.toCollection(HashSet::new));
        } else {
            deregisteredAccounts = dbSyncDataProvider.getDeregisteredAccountsInEpoch(epoch - 1, networkConfig.getExpectedSlotsPerEpoch());
            deregisteredAccountsOnEpochBoundary = deregisteredAccounts;
        }

        HashSet<String> sharedPoolRewardAddressesWithoutReward = new HashSet<>();
        if (epoch - 2 < networkConfig.getAllegraHardforkEpoch()) {
            sharedPoolRewardAddressesWithoutReward = dbSyncDataProvider.findSharedPoolRewardAddressWithoutReward(epoch - 2);
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

        HashSet<String> registeredAccountsSinceLastEpoch = dbSyncDataProvider.getRegisteredAccountsUntilLastEpoch(epoch, poolRewardAddresses, stabilityWindow);
        HashSet<String> registeredAccountsUntilNow = dbSyncDataProvider.getRegisteredAccountsUntilNow(epoch, poolRewardAddresses, stabilityWindow);

        HashSet<Reward> memberRewardsInEpoch = dbSyncDataProvider.getMemberRewardsInEpoch(epoch - 2);
        HashSet<PoolReward> totalPoolRewardsInEpoch = dbSyncDataProvider.getTotalPoolRewardsInEpoch(epoch - 2);

        HashSet<EpochValidationPoolReward> poolRewards = new HashSet<>();
        for (PoolReward poolReward : totalPoolRewardsInEpoch) {
            EpochValidationPoolReward epochValidationPoolReward = EpochValidationPoolReward.builder()
                    .poolId(poolReward.getPoolId())
                    .totalPoolReward(poolReward.getAmount())
                    .delegatorRewards(memberRewardsInEpoch.stream()
                            .filter(reward -> reward.getPoolId().equals(poolReward.getPoolId()))
                            .map(reward -> EpochValidationDelegatorReward.builder()
                                    .stakeAddress(reward.getStakeAddress())
                                    .reward(reward.getAmount())
                                    .build())
                            .collect(Collectors.toCollection(HashSet::new)))
                    .build();
            poolRewards.add(epochValidationPoolReward);
        }

        EpochValidationInput epochValidationInput = EpochValidationInput.builder()
                .epoch(epoch)
                .treasuryOfPreviousEpoch(treasuryOfPreviousEpoch)
                .reservesOfPreviousEpoch(reservesOfPreviousEpoch)
                .decentralisation(decentralisation)
                .treasuryGrowRate(treasuryGrowRate)
                .monetaryExpandRate(monetaryExpandRate)
                .optimalPoolCount(optimalPoolCount)
                .poolOwnerInfluence(poolOwnerInfluence)
                .fees(fees)
                .blockCount(blockCount)
                .activeStake(activeStake)
                .nonOBFTBlockCount(nonOBFTBlockCount)
                .rewardAddressesOfRetiredPoolsInEpoch(rewardAddressesOfRetiredPoolsInEpoch)
                .deregisteredAccounts(deregisteredAccounts)
                .lateDeregisteredAccounts(lateDeregisteredAccounts)
                .registeredAccountsSinceLastEpoch(registeredAccountsSinceLastEpoch)
                .registeredAccountsUntilNow(registeredAccountsUntilNow)
                .sharedPoolRewardAddressesWithoutReward(sharedPoolRewardAddressesWithoutReward)
                .deregisteredAccountsOnEpochBoundary(deregisteredAccountsOnEpochBoundary)
                .poolStates(new HashSet<>(poolStates))
                .poolRewards(poolRewards)
                .mirCertificates(new HashSet<>(mirCertificates))
                .build();
        try {
            JsonConverter.writeObjectToCompressedJsonFile(epochValidationInput, filePath);
        } catch (IOException e) {
            logger.error("Failed to write epoch validation input data to json file for epoch " + epoch);
        }
    }
}
