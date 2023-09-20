package org.cardanofoundation.rewards.data.fetcher;

import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rest.koios.client.backend.api.account.model.AccountUpdates;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.cardanofoundation.rewards.enums.DataType.*;
import static org.cardanofoundation.rewards.util.JsonConverter.writeObjectToJsonFile;

public class KoiosDataFetcher implements DataFetcher{

    private final KoiosDataProvider koiosDataProvider;
    private final JsonDataProvider jsonDataProvider;

    private static final Logger logger = LoggerFactory.getLogger(KoiosDataFetcher.class);

    public KoiosDataFetcher() {

        this.koiosDataProvider = new KoiosDataProvider();
        this.jsonDataProvider = new JsonDataProvider();
    }

    private void fetchAdaPots(int epoch, boolean override) {
        String filePath = String.format("./src/test/resources/%s/epoch%d.json", ADA_POTS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch AdaPots for epoch " + epoch + " because the json file already exists");
            return;
        }

        AdaPots adaPots = koiosDataProvider.getAdaPotsForEpoch(epoch);
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

    public void fetchEpochInfo(int epoch, boolean override) {
        String filePath = String.format("./src/test/resources/%s/epoch%d.json", EPOCH_INFO.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch EpochInfo for epoch " + epoch + " because the json file already exists");
            return;
        }

        Epoch epochInfo = koiosDataProvider.getEpochInfo(epoch);
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

    public void fetchProtocolParameters(int epoch, boolean override) {
        String filePath = String.format("./src/test/resources/%s/epoch%d.json", PROTOCOL_PARAMETERS.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch ProtocolParameters for epoch " + epoch + " because the json file already exists");
            return;
        }

        ProtocolParameters protocolParameters = koiosDataProvider.getProtocolParametersForEpoch(epoch);
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

    public void fetchAccountUpdates(int epoch, boolean override) {
        String filePath = String.format("./src/test/resources/%s/epoch%d.json", ACCOUNT_UPDATES.resourceFolderName, epoch);
        File outputFile = new File(filePath);

        if (outputFile.exists() && !override) {
            logger.info("Skip to fetch AccountUpdates for epoch " + epoch + " because the json file already exists");
            return;
        }

        List<String> stakeAddresses = jsonDataProvider.getStakeAddressesOfAllPoolsEverRetired();

        List<AccountUpdate> accountUpdates = koiosDataProvider.getAccountUpdatesUntilEpoch(stakeAddresses, epoch);
        if (accountUpdates == null) {
            logger.error("Failed to fetch AccountUpdates for epoch " + epoch);
            return;
        }

        if (accountUpdates.isEmpty()) {
            logger.info("Skip to write AccountUpdates to json file for epoch " + epoch + " because the accountUpdates are empty");
            return;
        }

        try {
            writeObjectToJsonFile(accountUpdates, filePath);
        } catch (IOException e) {
            logger.error("Failed to write AccountUpdates to json file for epoch " + epoch);
        }
    }

    @Override
    public void fetch(int epoch, boolean override) {
        fetchAdaPots(epoch, override);
        fetchEpochInfo(epoch, override);
        fetchProtocolParameters(epoch, override);
        fetchAccountUpdates(epoch, override);
    }
}
