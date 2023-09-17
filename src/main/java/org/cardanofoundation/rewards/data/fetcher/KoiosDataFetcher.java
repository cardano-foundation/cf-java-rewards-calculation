package org.cardanofoundation.rewards.data.fetcher;

import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.cardanofoundation.rewards.enums.DataType.*;
import static org.cardanofoundation.rewards.util.JsonConverter.writeObjectToJsonFile;

public class KoiosDataFetcher implements DataFetcher{

    private final KoiosDataProvider koiosDataProvider;

    private static final Logger logger = LoggerFactory.getLogger(KoiosDataFetcher.class);

    public KoiosDataFetcher() {
        this.koiosDataProvider = new KoiosDataProvider();
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

    @Override
    public void fetch(int epoch, boolean override) {
        fetchAdaPots(epoch, override);
        fetchEpochInfo(epoch, override);
        fetchProtocolParameters(epoch, override);
    }
}
