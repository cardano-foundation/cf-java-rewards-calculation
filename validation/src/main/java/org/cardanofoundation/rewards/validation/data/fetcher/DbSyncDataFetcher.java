package org.cardanofoundation.rewards.validation.data.fetcher;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.PoolBlocks;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.cardanofoundation.rewards.validation.enums.DataType.*;
import static org.cardanofoundation.rewards.validation.util.JsonConverter.writeObjectToJsonFile;

@Service
public class DbSyncDataFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DbSyncDataFetcher.class);
    @Autowired(required = false)
    private DbSyncDataProvider dbSyncDataProvider;

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

    private void fetchRetiredPoolsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, RETIRED_POOLS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch RetiredPools for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<PoolDeregistration> retiredPools = dbSyncDataProvider.getRetiredPoolsInEpoch(epoch);
        if (retiredPools == null) {
            logger.error("Failed to fetch RetiredPools for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(retiredPools, filePath);
        } catch (IOException e) {
            logger.error("Failed to write RetiredPools to json file for epoch " + epoch);
        }
    }

    private void fetchBlocksMadeByPoolsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, POOL_BLOCKS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch PoolsThatProducedBlocks for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<PoolBlocks> poolBlocks = dbSyncDataProvider.getBlocksMadeByPoolsInEpoch(epoch);

        if (poolBlocks == null) {
            logger.error("Failed to fetch PoolBlocks for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(poolBlocks, filePath);
        } catch (IOException e) {
            logger.error("Failed to write PoolsThatProducedBlocks to json file for epoch " + epoch);
        }
    }

    private void fetchHistoryOfAllPoolsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, POOL_HISTORY.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch HistoryOfAllPools for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<PoolBlocks> blocksMadeByPoolsInEpoch = dbSyncDataProvider.getBlocksMadeByPoolsInEpoch(epoch);
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
    }

    private void fetchLatestStakeAccountUpdates(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, ACCOUNT_UPDATES.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch LatestStakeAccountUpdates for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<AccountUpdate> accountUpdates = dbSyncDataProvider.getLatestStakeAccountUpdates(epoch);
        if (accountUpdates == null) {
            logger.error("Failed to fetch LatestStakeAccountUpdates for epoch " + epoch);
            return;
        }

        try {
            writeObjectToJsonFile(accountUpdates, filePath);
        } catch (IOException e) {
            logger.error("Failed to write LatestStakeAccountUpdates to json file for epoch " + epoch);
        }
    }

    private void fetchMemberRewardsInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, MEMBER_REWARDS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch MemberRewards for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<Reward> memberRewards = dbSyncDataProvider.getMemberRewardsInEpoch(epoch);
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

        List<TotalPoolRewards> totalPoolRewards = dbSyncDataProvider.getSumOfMemberAndLeaderRewardsInEpoch(epoch);
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

        List<String> sharedPoolRewardAddressesWithoutReward = dbSyncDataProvider.findSharedPoolRewardAddressWithoutReward(epoch);
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
    public void fetch(int epoch, boolean override) {
        fetchAdaPotsInEpoch(epoch, override);
        fetchEpochInfo(epoch, override);
        fetchProtocolParameters(epoch, override);
        fetchRetiredPoolsInEpoch(epoch, override);
        fetchBlocksMadeByPoolsInEpoch(epoch, override);
        fetchHistoryOfAllPoolsInEpoch(epoch, override);
        fetchLatestStakeAccountUpdates(epoch, override);
        fetchMemberRewardsInEpoch(epoch, override);
        fetchSumOfMemberAndLeaderRewardsInEpoch(epoch, override);
        fetchSharedPoolRewardAddressWithoutReward(epoch, override);
    }
}
