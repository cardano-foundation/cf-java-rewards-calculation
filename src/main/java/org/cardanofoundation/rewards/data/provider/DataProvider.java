package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.cardanofoundation.rewards.entity.PoolHistory;
import java.util.List;

public interface DataProvider {

    public AdaPots getAdaPotsForEpoch(int epoch);

    public Epoch getEpochInfo(int epoch);

    public ProtocolParameters getProtocolParametersForEpoch(int epoch);

    public PoolHistory getPoolHistory(String poolId, int epoch);

    public Double getPoolPledgeInEpoch(String poolId, int epoch);

    public List<String> getPoolOwners(String poolId, int epoch);

    public Double getActiveStakesOfAddressesInEpoch(List<String> stakeAddresses, int epoch);
}
