package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.DataType;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.cardanofoundation.rewards.enums.DataType.*;
import static org.cardanofoundation.rewards.util.JsonConverter.convertFileJsonToArrayList;
import static org.cardanofoundation.rewards.util.JsonConverter.readJsonFile;
@Service
public class JsonDataProvider implements DataProvider {

    private String getResourceFolder(DataType dataType, Integer epoch) {
        if (dataType.equals(POOL_DEREGISTRATIONS)) {
            return "./src/test/resources/poolDeregistrations/deregistrations.json";
        } else {
            return String.format("./src/test/resources/%s/epoch%d.json", dataType.resourceFolderName, epoch);
        }
    }

    private <T> T getDataFromJson(DataType dataType, int epoch, Class<T> objectClass) {
        String filePath = getResourceFolder(dataType, epoch);

        try {
            return readJsonFile(filePath, objectClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> List<T> getListFromJson(DataType dataType, Integer epoch, Class<T> objectClass) {
        String filePath = getResourceFolder(dataType, epoch);

        try {
            return convertFileJsonToArrayList(filePath, objectClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
        return null;
    }

    @Override
    public Double getPoolPledgeInEpoch(String poolId, int epoch) {
        return null;
    }

    @Override
    public List<String> getPoolOwners(String poolId, int epoch) {
        return null;
    }

    @Override
    public Double getActiveStakesOfAddressesInEpoch(List<String> stakeAddresses, int epoch) {
        return null;
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
                .filter(poolDeregistration -> poolDeregistration.getEpoch() == epoch)
                .toList();
    }

    @Override
    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch) {
        List<AccountUpdate> accountUpdates = getListFromJson(ACCOUNT_UPDATES, epoch, AccountUpdate.class);
        if (accountUpdates == null) return List.of();
        return accountUpdates.stream().filter(accountUpdate -> stakeAddresses.contains(accountUpdate.getStakeAddress())).toList();
    }
}
