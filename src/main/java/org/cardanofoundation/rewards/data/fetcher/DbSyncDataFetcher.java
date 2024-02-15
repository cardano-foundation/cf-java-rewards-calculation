package org.cardanofoundation.rewards.data.fetcher;

import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.entity.PoolHistory;
import org.cardanofoundation.rewards.entity.PoolOwnerHistory;
import org.cardanofoundation.rewards.entity.PoolParameters;
import org.cardanofoundation.rewards.entity.persistence.AggregationsInEpoch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.cardanofoundation.rewards.enums.DataType.AGGREGATIONS_IN_EPOCH;
import static org.cardanofoundation.rewards.util.JsonConverter.writeObjectToJsonFile;

@Service
public class DbSyncDataFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DbSyncDataFetcher.class);
    @Autowired
    private DbSyncDataProvider dbSyncDataProvider;

    @Value("${json.data-provider.source}")
    private String sourceFolder;

    private void aggregateValuesInEpoch(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, AGGREGATIONS_IN_EPOCH.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        File aggregationsInEpochFolder = new File(String.format("%s/%s", sourceFolder, AGGREGATIONS_IN_EPOCH.resourceFolderName));
        if (!aggregationsInEpochFolder.exists()) {
            if(!aggregationsInEpochFolder.mkdir()) {
                logger.error("Failed to create folder for aggregations in epoch");
                return;
            }
        }

        if (outputFile.exists() && !override) {
            logger.info("Skip to aggregate additional values for epoch " + epoch + " because the json file already exists");
            return;
        }

        BigInteger feeSum = dbSyncDataProvider.getSumOfFeesInEpoch(epoch);
        BigInteger withdrawalsSum = dbSyncDataProvider.getSumOfWithdrawalsInEpoch(epoch);
        BigInteger depositsSum = dbSyncDataProvider.getTransactionDepositsInEpoch(epoch);

        AggregationsInEpoch aggregationsInEpoch = new AggregationsInEpoch(feeSum, withdrawalsSum, depositsSum);

        try {
            writeObjectToJsonFile(aggregationsInEpoch, filePath);
        } catch (IOException e) {
            logger.error("Failed to write EpochInfo to json file for epoch " + epoch);
        }
    }

    private void fetchPoolHistoryByEpoch(String poolId, int epoch, boolean override) {
        String filePath = String.format("%s/pools/%s/history_epoch_%d.json", sourceFolder, poolId, epoch);
        File outputFile = new File(filePath);

        File poolIdFolder = new File(String.format("%s/pools/%s", sourceFolder, poolId));
        if (!poolIdFolder.exists()) {
            if(!poolIdFolder.mkdir()) {
                logger.error("Failed to create folder for pool " + poolId);
                return;
            }
        }

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch PoolHistory for pool " + poolId + " in epoch " + epoch + " because the json file already exists");
            return;
        }

        PoolHistory poolHistory = dbSyncDataProvider.getPoolHistory(poolId, epoch);

        if (poolHistory == null) {
            logger.error("PoolHistory for pool " + poolId + " in epoch " + epoch + " is null");
            return;
        }

        try {
            writeObjectToJsonFile(poolHistory, filePath);
        } catch (IOException e) {
            logger.error("Failed to write pool history to json file for epoch " + epoch);
        }
    }

    private void fetchPoolPledgeInEpoch (String poolId, int epoch, boolean override) {
        String filePath = String.format("%s/pools/%s/parameters_epoch_%d.json", sourceFolder, poolId, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch PoolPledge for pool " + poolId + " in epoch " + epoch + " because the json file already exists");
            return;
        }

        File poolIdFolder = new File(String.format("%s/pools/%s", sourceFolder, poolId));
        if (!poolIdFolder.exists()) {
            if(!poolIdFolder.mkdir()) {
                logger.error("Failed to create folder for pool " + poolId);
                return;
            }
        }

        Double poolPledge = dbSyncDataProvider.getPoolPledgeInEpoch(poolId, epoch);

        if (poolPledge == null) {
            logger.error("PoolPledge for pool " + poolId + " in epoch " + epoch + " is null");
            return;
        }

        PoolParameters poolParameters = PoolParameters.builder().epoch(epoch).pledge(poolPledge).build();

        try {
            writeObjectToJsonFile(poolParameters, filePath);
        } catch (IOException e) {
            logger.error("Failed to write pool params (pledge) to json file for epoch " + epoch);
        }
    }

    private void fetchPoolOwnersStakeInEpoch(String poolId, int epoch, boolean override) {
        String filePath = String.format("%s/pools/%s/owner_account_history_epoch_%d.json", sourceFolder, poolId, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch PoolOwnerStake for pool " + poolId + " in epoch " + epoch + " because the json file already exists");
            return;
        }

        File poolIdFolder = new File(String.format("%s/pools/%s", sourceFolder, poolId));
        if (!poolIdFolder.exists()) {
            if(!poolIdFolder.mkdir()) {
                logger.error("Failed to create folder for pool " + poolId);
                return;
            }
        }

        PoolOwnerHistory poolOwnersHistory = dbSyncDataProvider.getHistoryOfPoolOwnersInEpoch(poolId, epoch);

        if (poolOwnersHistory == null) {
            logger.info("Pool owners history for pool " + poolId + " in epoch " + epoch + " is null");
            return;
        }

        try {
            writeObjectToJsonFile(poolOwnersHistory, filePath);
        } catch (IOException e) {
            logger.error("Failed to write pool owner stake to json file for epoch " + epoch);
        }
    }

    @Override
    public void fetch(int epoch, boolean override) {
        aggregateValuesInEpoch(epoch, override);

        List<String> poolIds = List.of(
                "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9",
                "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt",
                "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx",
                "pool1spus7k8cy5qcs82xhw60dwwk2d4vrfs0m5vr2zst04gtq700gjn",
                "pool1qqqqx69ztfvd83rtafs3mk4lwanehrglmp2vwkjpaguecs2t4c2",
                "pool13n4jzw847sspllczxgnza7vkq80m8px7mpvwnsqthyy2790vmyc",
                "pool1ljlmfg7p37ysmea9ra5xqwccue203dpj40w6zlzn5r2cvjrf6tw"
        );

        for (String poolId : poolIds) {
            fetchPoolPledgeInEpoch(poolId, epoch, override);
            fetchPoolHistoryByEpoch(poolId, epoch, override);
            fetchPoolOwnersStakeInEpoch(poolId, epoch, override);
        }
    }
}
