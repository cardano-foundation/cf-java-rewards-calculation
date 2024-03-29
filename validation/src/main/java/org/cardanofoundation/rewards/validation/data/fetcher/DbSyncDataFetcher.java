package org.cardanofoundation.rewards.validation.data.fetcher;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;
import static org.cardanofoundation.rewards.validation.enums.DataType.*;
import static org.cardanofoundation.rewards.validation.util.JsonConverter.writeObjectToJsonFile;

@Service
public class DbSyncDataFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DbSyncDataFetcher.class);
    @Autowired(required = false)
    private DbSyncDataProvider dbSyncDataProvider;

    @Autowired(required = false)
    private JsonDataProvider jsonDataProvider;

    @Value("${json.data-provider.source}")
    private String sourceFolder;

    public void fetchAdaPotsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, ADA_POTS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch AdaPots for epoch " + epoch + " because the json file already exists");
            return;
        }

        AdaPots adaPots = dbSyncDataProvider.getAdaPotsForEpoch(epoch);
        if (adaPots == null) {
            logger.error("Failed to fetch AdaPots for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(adaPots, filePath);
        } catch (IOException e) {
            logger.error("Failed to write AdaPots to json file for epoch " + epoch);
        }
    }

    private void fetchProtocolParameters(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, PROTOCOL_PARAMETERS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch ProtocolParameters for epoch " + epoch + " because the json file already exists");
            return;
        }

        ProtocolParameters protocolParameters = dbSyncDataProvider.getProtocolParametersForEpoch(epoch);
        if (protocolParameters == null) {
            logger.error("Failed to fetch ProtocolParameters for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(protocolParameters, filePath);
        } catch (IOException e) {
            logger.error("Failed to write ProtocolParameters to json file for epoch " + epoch);
        }
    }

    private void fetchEpochInfo(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, EPOCH_INFO.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch EpochInfo for epoch " + epoch + " because the json file already exists");
            return;
        }

        Epoch epochInfo = dbSyncDataProvider.getEpochInfo(epoch);
        if (epochInfo == null) {
            logger.error("Failed to fetch EpochInfo for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(epochInfo, filePath);
        } catch (IOException e) {
            logger.error("Failed to write EpochInfo to json file for epoch " + epoch);
        }
    }

    private void fetchMirCertificatesInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, MIR_CERTIFICATE.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch MirCertificates for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<MirCertificate> mirCertificates = dbSyncDataProvider.getMirCertificatesInEpoch(epoch);
        if (mirCertificates == null) {
            logger.error("Failed to fetch MirCertificates for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(mirCertificates, filePath);
        } catch (IOException e) {
            logger.error("Failed to write MirCertificates to json file for epoch " + epoch);
        }
    }

    private void fetchRetiredPoolsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, REWARD_ADDRESSES_OF_RETIRED_POOLS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch RetiredPools for epoch " + epoch + " because the json file already exists");
            return;
        }

        HashSet<String> rewardAddressesOfRetiredPoolsInEpoch = dbSyncDataProvider.getRewardAddressesOfRetiredPoolsInEpoch(epoch);
        if (rewardAddressesOfRetiredPoolsInEpoch == null) {
            logger.error("Failed to fetch reward addresses of retired pools for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(rewardAddressesOfRetiredPoolsInEpoch, filePath);
        } catch (IOException e) {
            logger.error("Failed to fetch reward addresses of retired pools to json file for epoch " + epoch);
        }
    }

    private void fetchStakeAddressesWithRegistrationsUntilEpoch(int epoch, boolean override, boolean mainnet) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, PAST_ACCOUNT_REGISTRATIONS_UNTIL_LAST_EPOCH.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch StakeAddressesWithRegistrationsUntilEpoch for epoch " + epoch + " because the json file already exists");
            return;
        }

        long stabilityWindow = EXPECTED_SLOTS_PER_EPOCH;
        if (mainnet && (epoch - 2) < MAINNET_VASIL_HARDFORK_EPOCH) {
            stabilityWindow = RANDOMNESS_STABILISATION_WINDOW;
        }

        List<PoolHistory> poolHistories = jsonDataProvider.getHistoryOfAllPoolsInEpoch(epoch - 2, null);
        HashSet<String> poolRewardAddresses = poolHistories.stream().map(PoolHistory::getRewardAddress).collect(Collectors.toCollection(HashSet::new));
        HashSet<String> rewardAddressesOfRetiredPools = jsonDataProvider.getRewardAddressesOfRetiredPoolsInEpoch(epoch);
        poolRewardAddresses.addAll(rewardAddressesOfRetiredPools);

        HashSet<String> accountsRegisteredInThePast = dbSyncDataProvider.getRegisteredAccountsUntilLastEpoch(epoch, poolRewardAddresses, stabilityWindow);
        if (accountsRegisteredInThePast == null) {
            logger.error("Failed to fetch StakeAddressesWithRegistrationsUntilEpoch for epoch " + epoch);
        }

        try {
            writeObjectToJsonFile(accountsRegisteredInThePast, filePath);
        } catch (IOException e) {
            logger.error("Failed to write accounts registered in the past to json file for epoch " + epoch);
        }

        HashSet<String> accountsRegisteredUntilNow = dbSyncDataProvider.getRegisteredAccountsUntilNow(epoch, poolRewardAddresses, stabilityWindow);
        filePath = String.format("%s/%s/epoch%d.json", sourceFolder, PAST_ACCOUNT_REGISTRATIONS_UNTIL_NOW.resourceFolderName, epoch);
        outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch accountsRegisteredUntilNow for epoch " + epoch + " because the json file already exists");
            return;
        }

        try {
            writeObjectToJsonFile(accountsRegisteredUntilNow, filePath);
        } catch (IOException e) {
            logger.error("Failed to write accounts registered until now to json file for epoch " + epoch);
        }
    }

    private void fetchHistoryOfAllPoolsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, POOL_HISTORY.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch HistoryOfAllPools for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<PoolBlock> blocksMadeByPoolsInEpoch = dbSyncDataProvider.getBlocksMadeByPoolsInEpoch(epoch);
        List<PoolHistory> poolHistories = dbSyncDataProvider.getHistoryOfAllPoolsInEpoch(epoch, blocksMadeByPoolsInEpoch);
        if (poolHistories == null) {
            logger.error("Failed to fetch HistoryOfAllPools for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(poolHistories, filePath);
        } catch (IOException e) {
            logger.error("Failed to write HistoryOfAllPools to json file for epoch " + epoch);
        }

        filePath = String.format("%s/%s/epoch%d.json", sourceFolder, POOL_BLOCKS.resourceFolderName, epoch);

        try {
            writeObjectToJsonFile(blocksMadeByPoolsInEpoch, filePath);
        } catch (IOException e) {
            logger.error("Failed to write blocks made by pools to json file for epoch " + epoch);
        }
    }

    private void fetchDeregisteredAccountsInEpoch(int epoch, boolean override, boolean mainnet) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, ACCOUNT_DEREGISTRATION.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch deregistered accounts at epoch boundary for epoch " + epoch + " because the json file already exists");
        } else {
            HashSet<String> deregisteredAccountsInEpoch = dbSyncDataProvider.getDeregisteredAccountsInEpoch(epoch, EXPECTED_SLOTS_PER_EPOCH);
            if (deregisteredAccountsInEpoch == null) {
                logger.error("Failed to fetch deregistered accounts at epoch boundary for epoch " + epoch);
                return;
            }

            try {
                writeObjectToJsonFile(deregisteredAccountsInEpoch, filePath);
            } catch (IOException e) {
                logger.error("Failed to write deregistered accounts in epoch to json file for epoch " + epoch);
            }
        }

        if (mainnet && (epoch - 2) < MAINNET_VASIL_HARDFORK_EPOCH) {
            filePath = String.format("%s/%s/epoch%d-stability-window.json", sourceFolder, ACCOUNT_DEREGISTRATION.resourceFolderName, epoch);
            outputFile = new File(filePath);

            if (outputFile.exists() && !override) {
                logger.info("Skip to fetch deregistered accounts at epoch boundary for epoch " + epoch + " because the json file already exists");
                return;
            }

            HashSet<String> deregisteredAccountsInEpoch = dbSyncDataProvider.getDeregisteredAccountsInEpoch(epoch, RANDOMNESS_STABILISATION_WINDOW);
            if (deregisteredAccountsInEpoch == null) {
                logger.error("Failed to fetch deregistered accounts at stabilisation window for epoch " + epoch);
                return;
            }

            try {
                writeObjectToJsonFile(deregisteredAccountsInEpoch, filePath);
            } catch (IOException e) {
                logger.error("Failed to write deregistered accounts in epoch at stabilisation window to json file for epoch " + epoch);
            }
        }
    }

    private void fetchMemberRewardsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, MEMBER_REWARDS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch MemberRewards for epoch " + epoch + " because the json file already exists");
            return;
        }

        HashSet<Reward> memberRewards = dbSyncDataProvider.getMemberRewardsInEpoch(epoch);
        if (memberRewards == null) {
            logger.error("Failed to fetch MemberRewards for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(memberRewards, filePath);
        } catch (IOException e) {
            logger.error("Failed to write MemberRewards to json file for epoch " + epoch);
        }
    }

    private void fetchSumOfMemberAndLeaderRewardsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, REWARDS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch SumOfMemberAndLeaderRewards for epoch " + epoch + " because the json file already exists");
            return;
        }

        HashSet<PoolReward> totalPoolRewards = dbSyncDataProvider.getTotalPoolRewardsInEpoch(epoch);
        if (totalPoolRewards == null) {
            logger.error("Failed to fetch SumOfMemberAndLeaderRewards for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(totalPoolRewards, filePath);
        } catch (IOException e) {
            logger.error("Failed to write SumOfMemberAndLeaderRewards to json file for epoch " + epoch);
        }
    }

    private void fetchSharedPoolRewardAddressWithoutReward(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, REWARDS_OUTLIER.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch SharedPoolRewardAddressWithoutReward for epoch " + epoch + " because the json file already exists");
            return;
        }

        HashSet<String> sharedPoolRewardAddressesWithoutReward = dbSyncDataProvider.findSharedPoolRewardAddressWithoutReward(epoch);
        if (sharedPoolRewardAddressesWithoutReward == null) {
            logger.error("Failed to fetch SharedPoolRewardAddressWithoutReward for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(sharedPoolRewardAddressesWithoutReward, filePath);
        } catch (IOException e) {
            logger.error("Failed to write SharedPoolRewardAddressWithoutReward to json file for epoch " + epoch);
        }
    }

    @Override
    public void fetch(int epoch, boolean override, boolean skipValidationData) {
        fetchAdaPotsInEpoch(epoch, override);
        fetchEpochInfo(epoch, override);
        fetchProtocolParameters(epoch, override);
        fetchRetiredPoolsInEpoch(epoch, override);
        fetchHistoryOfAllPoolsInEpoch(epoch, override);
        fetchDeregisteredAccountsInEpoch(epoch, override, true);
        fetchSharedPoolRewardAddressWithoutReward(epoch, override);
        fetchMirCertificatesInEpoch(epoch, override);
        fetchStakeAddressesWithRegistrationsUntilEpoch(epoch, override, true);

        if (!skipValidationData) {
            fetchMemberRewardsInEpoch(epoch, override);
            fetchSumOfMemberAndLeaderRewardsInEpoch(epoch, override);
        }
    }
}
