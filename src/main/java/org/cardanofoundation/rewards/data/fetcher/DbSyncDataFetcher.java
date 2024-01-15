package org.cardanofoundation.rewards.data.fetcher;

import org.cardanofoundation.rewards.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.entity.persistence.AggregationsInEpoch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;

import static org.cardanofoundation.rewards.enums.DataType.AGGREGATIONS_IN_EPOCH;
import static org.cardanofoundation.rewards.util.JsonConverter.writeObjectToJsonFile;

public class DbSyncDataFetcher implements DataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(DbSyncDataFetcher.class);
    private final DbSyncDataProvider dbSyncDataProvider;

    public DbSyncDataFetcher() {
        this.dbSyncDataProvider = new DbSyncDataProvider();
    }

    private void aggregateValuesInEpoch(int epoch, boolean override) {
        String filePath = String.format("./src/test/resources/%s/epoch%d.json", AGGREGATIONS_IN_EPOCH.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to aggregate additional values for epoch " + epoch + " because the json file already exists");
            return;
        }

        double feeSum = dbSyncDataProvider.getSumOfFeesInEpoch(epoch);
        double withdrawalsSum = dbSyncDataProvider.getSumOfWithdrawalsInEpoch(epoch);
        double depositsSum = dbSyncDataProvider.getTransactionDepositsInEpoch(epoch);

        AggregationsInEpoch aggregationsInEpoch = new AggregationsInEpoch(feeSum, withdrawalsSum, depositsSum);

        try {
            writeObjectToJsonFile(aggregationsInEpoch, filePath);
        } catch (IOException e) {
            logger.error("Failed to write EpochInfo to json file for epoch " + epoch);
        }
    }

    @Override
    public void fetch(int epoch, boolean override) {
        aggregateValuesInEpoch(epoch, override);
    }
}
