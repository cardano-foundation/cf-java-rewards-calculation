package org.cardanofoundation.rewards.validation.data.fetcher;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.KoiosDataProvider;
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
public class KoiosDataFetcher implements DataFetcher {

    @Autowired
    private KoiosDataProvider koiosDataProvider;
    @Autowired
    private JsonDataProvider jsonDataProvider;

    @Value("${json.data-provider.source}")
    private String sourceFolder;

    private static final Logger logger = LoggerFactory.getLogger(KoiosDataFetcher.class);

    private void fetchAdaPots(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, ADA_POTS.resourceFolderName, epoch);
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

    private void fetchEpochInfo(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, EPOCH_INFO.resourceFolderName, epoch);
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

    private void fetchProtocolParameters(int epoch, boolean override) {
        String filePath = String.format("%s/%s/epoch%d.json", sourceFolder, PROTOCOL_PARAMETERS.resourceFolderName, epoch);
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

        PoolHistory poolHistory = koiosDataProvider.getPoolHistory(poolId, epoch);

        try {
            writeObjectToJsonFile(poolHistory, filePath);
        } catch (IOException e) {
            logger.error("Failed to write pool history to json file for epoch " + epoch);
        }
    }

    @Override
    public void fetch(int epoch, boolean override, boolean skipValidationData) {
        fetchAdaPots(epoch, override);
        fetchEpochInfo(epoch, override);
        fetchProtocolParameters(epoch, override);

        List<String> poolIds = List.of(
            "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9",
            "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt",
            "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx",
            "pool1spus7k8cy5qcs82xhw60dwwk2d4vrfs0m5vr2zst04gtq700gjn",
            "pool1qqqqx69ztfvd83rtafs3mk4lwanehrglmp2vwkjpaguecs2t4c2",
            "pool13n4jzw847sspllczxgnza7vkq80m8px7mpvwnsqthyy2790vmyc",
            "pool1ljlmfg7p37ysmea9ra5xqwccue203dpj40w6zlzn5r2cvjrf6tw",
            "pool19ctjr5ft75sz396hn0tf6ns4hy5w9l9jp2jh3m8mx6acvm2cn7j"
        );

        for (String poolId : poolIds) {
            fetchPoolHistoryByEpoch(poolId, epoch, override);
        }
    }
}
