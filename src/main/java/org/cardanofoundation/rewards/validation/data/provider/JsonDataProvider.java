package org.cardanofoundation.rewards.validation.data.provider;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.PoolBlocks;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;
import org.cardanofoundation.rewards.validation.enums.DataType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.cardanofoundation.rewards.validation.enums.DataType.*;
import static org.cardanofoundation.rewards.validation.util.JsonConverter.convertFileJsonToArrayList;
import static org.cardanofoundation.rewards.validation.util.JsonConverter.readJsonFile;
@Service
public class JsonDataProvider implements DataProvider {

    @Value("${json.data-provider.source}")
    private String sourceFolder;

    private String getResourceFolder(DataType dataType, Integer epoch, String poolId) {
        if (sourceFolder == null) throw new RuntimeException("Invalid source folder for JSON data provider. Please check the JSON_DATA_SOURCE_FOLDER environment variable that you have provided.");

        if (dataType.equals(POOL_DEREGISTRATIONS)) {
            return String.join(File.separator, sourceFolder, POOL_DEREGISTRATIONS.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(ACCOUNT_UPDATES)) {
            return String.join(File.separator, sourceFolder, ACCOUNT_UPDATES.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(REWARDS_OUTLIER)) {
            return String.join(File.separator, sourceFolder, REWARDS_OUTLIER.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(ACCOUNT_DEREGISTRATION)) {
            return String.join(File.separator, sourceFolder, ACCOUNT_DEREGISTRATION.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(LATE_DEREGISTRATIONS)) {
            return String.join(File.separator, sourceFolder, LATE_DEREGISTRATIONS.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(PAST_ACCOUNT_REGISTRATIONS)) {
            return String.join(File.separator, sourceFolder, PAST_ACCOUNT_REGISTRATIONS.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(POOL_BLOCKS)) {
            return String.join(File.separator, sourceFolder, POOL_BLOCKS.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(ADA_POTS)) {
            return String.join(File.separator, sourceFolder, ADA_POTS.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(EPOCH_INFO)) {
            return String.join(File.separator, sourceFolder, EPOCH_INFO.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(PROTOCOL_PARAMETERS)) {
            return String.join(File.separator, sourceFolder, PROTOCOL_PARAMETERS.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(POOL_PARAMETERS)) {
            return String.join(File.separator, sourceFolder, POOL_PARAMETERS.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(POOL_HISTORY)) {
            return String.join(File.separator, sourceFolder, POOL_HISTORY.resourceFolderName, "epoch" + epoch + ".json");
        } else if (dataType.equals(POOL_OWNER_HISTORY)) {
            return String.join(File.separator, sourceFolder, "pools", poolId, "owner_account_history_epoch_" + epoch + ".json");
        } else if (dataType.equals(MIR_CERTIFICATE)) {
            return String.join(File.separator, sourceFolder, MIR_CERTIFICATE.resourceFolderName, "epoch" + epoch + ".json");
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

    private <T> HashSet<T> getHashSetFromJson(DataType dataType, Integer epoch, Class<T> objectClass) {
        String filePath = getResourceFolder(dataType, epoch, null);

        try {
            return new HashSet<>(Objects.requireNonNull(convertFileJsonToArrayList(filePath, objectClass)));
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
    public List<PoolHistory> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch) {
        List<PoolHistory> histories = getListFromJson(POOL_HISTORY, epoch, PoolHistory.class);
        if (histories == null) return List.of();
        return histories;
    }

    @Override
    public PoolHistory getPoolHistory(String poolId, int epoch) {
        return getDataFromJson(POOL_HISTORY, epoch, PoolHistory.class, poolId);
    }

    @Override
    public BigInteger getPoolPledgeInEpoch(String poolId, int epoch) {
        PoolParameters poolParameters = getDataFromJson(POOL_PARAMETERS, epoch, PoolParameters.class, poolId);

        if (poolParameters == null) return null;

        return BigInteger.valueOf(poolParameters.getPledge().longValue());
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
        List<PoolDeregistration> poolDeregistrations = getListFromJson(RETIRED_POOLS, epoch, PoolDeregistration.class);
        if (poolDeregistrations == null) return List.of();
        return poolDeregistrations;
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
        if (mirCertificates == null) return List.of();
        return mirCertificates;
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
    public BigInteger getTransactionDepositsInEpoch(int epoch) {
        return null;
    }

    @Override
    public BigInteger getSumOfFeesInEpoch(int epoch) {
        return null;
    }

    @Override
    public BigInteger getSumOfWithdrawalsInEpoch(int epoch) {
        return null;
    }

    @Override
    public List<Reward> getMemberRewardsInEpoch(int epoch) {
        return null;
    }

    @Override
    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch) {
        List<PoolBlock> poolBlocks = getListFromJson(POOL_BLOCKS, epoch, PoolBlock.class);
        if (poolBlocks == null) return List.of();
        return poolBlocks;
    }

    @Override
    public List<AccountUpdate> getLatestStakeAccountUpdates(int epoch) {
        return null;
    }

    @Override
    public List<TotalPoolRewards> getSumOfMemberAndLeaderRewardsInEpoch(int epoch) {
        return null;
    }

    @Override
    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch) {
        HashSet<String> sharedPoolRewardAddressesWithoutReward = getHashSetFromJson(REWARDS_OUTLIER, epoch, String.class);
        if (sharedPoolRewardAddressesWithoutReward == null) return new HashSet<>();
        return sharedPoolRewardAddressesWithoutReward;
    }

    @Override
    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow) {
        HashSet<String> deregisteredAccounts = getHashSetFromJson(ACCOUNT_DEREGISTRATION, epoch, String.class);
        if (deregisteredAccounts == null) return new HashSet<>();
        return deregisteredAccounts;
    }

    @Override
    public HashSet<String> getLateAccountDeregistrationsInEpoch(int epoch, long stabilityWindow) {
        HashSet<String> lateDeregisteredAccounts = getHashSetFromJson(LATE_DEREGISTRATIONS, epoch, String.class);
        if (lateDeregisteredAccounts == null) return new HashSet<>();
        return lateDeregisteredAccounts;
    }

    @Override
    public HashSet<String> getStakeAddressesWithRegistrationsUntilEpoch(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        HashSet<String> accountsRegisteredInThePast = getHashSetFromJson(PAST_ACCOUNT_REGISTRATIONS, epoch, String.class);
        if (accountsRegisteredInThePast == null) return new HashSet<>();
        return accountsRegisteredInThePast;
    }
}
