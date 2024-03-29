package org.cardanofoundation.rewards.validation.data.provider;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.validation.enums.DataType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;
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
        } else if (dataType.equals(DEREGISTRATIONS_ON_STABILITY_WINDOW)) {
            return String.join(File.separator, sourceFolder, ACCOUNT_DEREGISTRATION.resourceFolderName, "epoch" + epoch + "-stability-window.json");
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
            ArrayList<T> objectList = convertFileJsonToArrayList(filePath, objectClass);
            if (objectList == null) return null;
            return new HashSet<>(objectList);
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
        if (epoch < MAINNET_SHELLEY_START_EPOCH) {
            return AdaPots.builder()
                    .treasury(BigInteger.ZERO)
                    .reserves(BigInteger.ZERO)
                    .rewards(BigInteger.ZERO)
                    .epoch(epoch)
                    .build();
        } else if (epoch == MAINNET_SHELLEY_START_EPOCH) {
            return AdaPots.builder()
                    .treasury(MAINNET_SHELLEY_INITIAL_TREASURY)
                    .reserves(MAINNET_SHELLEY_INITIAL_RESERVES)
                    .rewards(BigInteger.ZERO)
                    .epoch(epoch)
                    .build();
        }

        return getDataFromJson(ADA_POTS, epoch, AdaPots.class);
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) return null;

        return getDataFromJson(EPOCH_INFO, epoch, Epoch.class);
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) return new ProtocolParameters();

        return getDataFromJson(PROTOCOL_PARAMETERS, epoch, ProtocolParameters.class);
    }

    @Override
    public List<PoolHistory> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) return List.of();

        List<PoolHistory> histories = getListFromJson(POOL_HISTORY, epoch, PoolHistory.class);
        if (histories == null) return List.of();
        return histories;
    }

    @Override
    public PoolHistory getPoolHistory(String poolId, int epoch) {
        List<PoolHistory> poolHistories = getHistoryOfAllPoolsInEpoch(epoch, null);
        return poolHistories.stream().filter(history -> history.getPoolId().equals(poolId)).findFirst().orElse(null);
    }

    @Override
    public HashSet<String> getRewardAddressesOfRetiredPoolsInEpoch(int epoch) {
        HashSet<String> poolDeregistrations = getHashSetFromJson(REWARD_ADDRESSES_OF_RETIRED_POOLS, epoch, String.class);
        if (poolDeregistrations == null) return new HashSet<>();
        return poolDeregistrations;
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
    public HashSet<Reward> getMemberRewardsInEpoch(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) return new HashSet<>();

        HashSet<Reward> rewards = getHashSetFromJson(MEMBER_REWARDS, epoch, Reward.class);
        if (rewards == null) return new HashSet<>();
        return rewards;
    }

    @Override
    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) return List.of();

        List<PoolBlock> poolBlocks = getListFromJson(POOL_BLOCKS, epoch, PoolBlock.class);
        if (poolBlocks == null) return List.of();
        return poolBlocks;
    }

    @Override
    public HashSet<PoolReward> getTotalPoolRewardsInEpoch(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) return new HashSet<>();

        HashSet<PoolReward> totalPoolRewards = getHashSetFromJson(REWARDS, epoch, PoolReward.class);
        if (totalPoolRewards == null) return new HashSet<>();
        return totalPoolRewards;
    }

    @Override
    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) return new HashSet<>();

        HashSet<String> sharedPoolRewardAddressesWithoutReward = getHashSetFromJson(REWARDS_OUTLIER, epoch, String.class);
        if (sharedPoolRewardAddressesWithoutReward == null) return new HashSet<>();
        return sharedPoolRewardAddressesWithoutReward;
    }

    @Override
    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow) {
        HashSet<String> deregisteredAccounts;
        if (stabilityWindow == RANDOMNESS_STABILISATION_WINDOW) {
            deregisteredAccounts = getHashSetFromJson(DEREGISTRATIONS_ON_STABILITY_WINDOW, epoch, String.class);
        } else {
            deregisteredAccounts = getHashSetFromJson(ACCOUNT_DEREGISTRATION, epoch, String.class);
        }

        if (deregisteredAccounts == null) return new HashSet<>();
        return deregisteredAccounts;
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilLastEpoch(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        HashSet<String> accountsRegisteredInThePast = getHashSetFromJson(PAST_ACCOUNT_REGISTRATIONS_UNTIL_LAST_EPOCH, epoch, String.class);
        if (accountsRegisteredInThePast == null) return new HashSet<>();
        return accountsRegisteredInThePast;
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilNow(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        HashSet<String> accountsRegisteredInThePast = getHashSetFromJson(PAST_ACCOUNT_REGISTRATIONS_UNTIL_NOW, epoch, String.class);
        if (accountsRegisteredInThePast == null) return new HashSet<>();
        return accountsRegisteredInThePast;
    }
}
