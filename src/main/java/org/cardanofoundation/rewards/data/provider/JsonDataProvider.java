package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.DataType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

import static org.cardanofoundation.rewards.enums.DataType.*;
import static org.cardanofoundation.rewards.util.JsonConverter.convertFileJsonToArrayList;
import static org.cardanofoundation.rewards.util.JsonConverter.readJsonFile;
@Service
public class JsonDataProvider implements DataProvider {

    @Value("${json.data-provider.source}")
    private String sourceFolder;

    private String getResourceFolder(DataType dataType, Integer epoch, String poolId) {
        if (sourceFolder == null) throw new RuntimeException("Invalid source folder for JSON data provider. Please check the JSON_DATA_SOURCE_FOLDER environment variable that you have provided.");

        if (dataType.equals(POOL_DEREGISTRATIONS)) {
            return String.join(File.separator, sourceFolder, "poolDeregistrations", "deregistrations.json");
        } else if (dataType.equals(POOL_PARAMETERS)) {
            return String.join(File.separator, sourceFolder, "pools", poolId, "parameters_epoch_" + epoch + ".json");
        } else if (dataType.equals(POOL_HISTORY)) {
            return String.join(File.separator, sourceFolder, "pools", poolId, "history_epoch_" + epoch + ".json");
        } else if (dataType.equals(POOL_OWNER_HISTORY)) {
            return String.join(File.separator, sourceFolder, "pools", poolId, "owner_account_history_epoch_" + epoch + ".json");
        } else if (dataType.equals(MIR_CERTIFICATE)) {
            return String.join(File.separator, sourceFolder, "mirCertificates", "mirCertificates.json");
        } else {
            return String.join(File.separator, sourceFolder, dataType.resourceFolderName, "epoch" + epoch + ".json");
        }
    }

    private <T> T getDataFromJson(DataType dataType, int epoch, Class<T> objectClass, String poolId) {
        String filePath = getResourceFolder(dataType, epoch, poolId);

        try {
            return readJsonFile(filePath, objectClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> T getDataFromJson(DataType dataType, int epoch, Class<T> objectClass) {
        return getDataFromJson(dataType, epoch, objectClass, null);
    }

    private <T> List<T> getListFromJson(DataType dataType, Integer epoch, Class<T> objectClass, String poolId) {
        String filePath = getResourceFolder(dataType, epoch, poolId);

        try {
            return convertFileJsonToArrayList(filePath, objectClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> List<T> getListFromJson(DataType dataType, Integer epoch, Class<T> objectClass) {
        return getListFromJson(dataType, epoch, objectClass, null);
    }

    @Override
    public AdaPots getAdaPotsForEpoch(int epoch) {
        return getDataFromJson(ADA_POTS, epoch, AdaPots.class);
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        return getDataFromJson(EPOCH_INFO, epoch, Epoch.class);
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        return getDataFromJson(PROTOCOL_PARAMETERS, epoch, ProtocolParameters.class);
    }

    @Override
    public PoolHistory getPoolHistory(String poolId, int epoch) {
        return getDataFromJson(POOL_HISTORY, epoch, PoolHistory.class, poolId);
    }

    @Override
    public Double getPoolPledgeInEpoch(String poolId, int epoch) {
        PoolParameters poolParameters = getDataFromJson(POOL_PARAMETERS, epoch, PoolParameters.class, poolId);

        if (poolParameters == null) return null;

        return poolParameters.getPledge();
    }

    @Override
    public PoolOwnerHistory getHistoryOfPoolOwnersInEpoch(String poolId, int epoch) {
        return getDataFromJson(POOL_OWNER_HISTORY, epoch, PoolOwnerHistory.class, poolId);
    }

    public List<String> getStakeAddressesOfAllPoolsEverRetired() {
        List<PoolDeregistration> poolDeregistrations = getListFromJson(POOL_DEREGISTRATIONS, null, PoolDeregistration.class);

        if (poolDeregistrations == null) return List.of();

        return poolDeregistrations.stream()
                .map(PoolDeregistration::getRewardAddress)
                .toList();
    }

    @Override
    public List<PoolDeregistration> getRetiredPoolsInEpoch(int epoch) {
        List<PoolDeregistration> poolDeregistrations = getListFromJson(POOL_DEREGISTRATIONS, null, PoolDeregistration.class);
        if (poolDeregistrations == null) return List.of();

        return poolDeregistrations.stream()
                .filter(poolDeregistration -> poolDeregistration.getRetiringEpoch() == epoch)
                .toList();
    }

    @Override
    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch) {
        List<AccountUpdate> accountUpdates = getListFromJson(ACCOUNT_UPDATES, epoch, AccountUpdate.class);
        if (accountUpdates == null) return List.of();
        return accountUpdates.stream().filter(accountUpdate -> stakeAddresses.contains(accountUpdate.getStakeAddress())).toList();
    }

    @Override
    public List<MirCertificate> getMirCertificatesInEpoch(int epoch) {
        List<MirCertificate> mirCertificates = getListFromJson(MIR_CERTIFICATE, epoch, MirCertificate.class);
        Epoch epochInfo = getDataFromJson(EPOCH_INFO, epoch, Epoch.class);
        if (mirCertificates == null || epochInfo == null) return List.of();

        return mirCertificates.stream()
                .filter(mirCertificate -> mirCertificate.getBlockTime() <= epochInfo.getUnixTimeLastBlock())
                .filter(mirCertificate -> mirCertificate.getBlockTime() >= epochInfo.getUnixTimeFirstBlock())
                .toList();
    }

    @Override
    public int getPoolRegistrationsInEpoch(int epoch) {
        return 0;
    }

    @Override
    public List<PoolUpdate> getPoolUpdateAfterTransactionIdInEpoch(String poolId, long transactionId, int epoch) {
        return null;
    }

    @Override
    public PoolDeregistration latestPoolRetirementUntilEpoch(String poolId, int epoch) {
        return null;
    }

    @Override
    public Double getTransactionDepositsInEpoch(int epoch) {
        return null;
    }

    @Override
    public Double getSumOfFeesInEpoch(int epoch) {
        return null;
    }

    @Override
    public Double getSumOfWithdrawalsInEpoch(int epoch) {
        return null;
    }

    @Override
    public List<Reward> getRewardListForPoolInEpoch(int epoch, String poolId) {
        return null;
    }

    @Override
    public Double getTotalPoolRewardsInEpoch(String poolId, int epoch) {
        return null;
    }
}
