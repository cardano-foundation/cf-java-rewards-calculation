package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;

import java.util.List;

public interface DataProvider {

    public AdaPots getAdaPotsForEpoch(int epoch);

    public Epoch getEpochInfo(int epoch);

    public ProtocolParameters getProtocolParametersForEpoch(int epoch);

    public PoolHistory getPoolHistory(String poolId, int epoch);

    public Double getPoolPledgeInEpoch(String poolId, int epoch);

    public List<String> getPoolOwners(String poolId, int epoch);

    public Double getActiveStakesOfAddressesInEpoch(List<String> stakeAddresses, int epoch);

    public List<PoolUpdate> getPoolUpdatesInEpoch(int epoch);

    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch);
}
