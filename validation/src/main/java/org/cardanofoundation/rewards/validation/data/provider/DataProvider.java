package org.cardanofoundation.rewards.validation.data.provider;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.domain.PoolReward;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface DataProvider {

    public AdaPots getAdaPotsForEpoch(int epoch);

    public Epoch getEpochInfo(int epoch, NetworkConfig networkConfig);

    public ProtocolParameters getProtocolParametersForEpoch(int epoch);

    public List<PoolState> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch);
    public PoolState getPoolHistory(String poolId, int epoch);

    public Set<RetiredPool> getRetiredPoolsInEpoch(int epoch);

    public List<MirCertificate> getMirCertificatesInEpoch(int epoch);

    public BigInteger getTransactionDepositsInEpoch(int epoch);

    public BigInteger getSumOfFeesInEpoch(int epoch);

    public BigInteger getSumOfWithdrawalsInEpoch(int epoch);

    public HashSet<Reward> getMemberRewardsInEpoch(int epoch);

    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch);

    public HashSet<PoolReward> getTotalPoolRewardsInEpoch(int epoch);

    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch);

    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow);

    public HashSet<String> getRegisteredAccountsUntilLastEpoch(Integer epoch, HashSet<String> stakeAddresses,
                                                               Long stabilityWindow);

    HashSet<String> getRegisteredAccountsUntilNow(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow);
}
