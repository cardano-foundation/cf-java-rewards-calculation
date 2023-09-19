package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.DataType;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.cardanofoundation.rewards.enums.DataType.*;
import static org.cardanofoundation.rewards.util.JsonConverter.readJsonFile;
@Service
public class JsonDataProvider implements DataProvider {

    private <T> T getDataFromJson(String resourceFolderName, int epoch, Class<T> objectClass) {
        String filePath = String.format("./src/test/resources/%s/epoch%d.json", resourceFolderName, epoch);

        try {
            return readJsonFile(filePath, objectClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public AdaPots getAdaPotsForEpoch(int epoch) {
        return getDataFromJson(ADA_POTS.resourceFolderName, epoch, AdaPots.class);
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        return getDataFromJson(EPOCH_INFO.resourceFolderName, epoch, Epoch.class);
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        return getDataFromJson(PROTOCOL_PARAMETERS.resourceFolderName, epoch, ProtocolParameters.class);
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

    @Override
    public List<PoolUpdate> getPoolUpdatesInEpoch(int epoch) {
        return null;
    }

    @Override
    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch) {
        return null;
    }
}
