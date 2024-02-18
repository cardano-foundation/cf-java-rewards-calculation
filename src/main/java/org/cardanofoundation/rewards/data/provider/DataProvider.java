package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.entity.jpa.projection.LatestStakeAccountUpdate;

import java.math.BigInteger;
import java.util.List;

public interface DataProvider {

    public AdaPots getAdaPotsForEpoch(int epoch);

    public Epoch getEpochInfo(int epoch);

    public ProtocolParameters getProtocolParametersForEpoch(int epoch);

    public List<PoolHistory> getHistoryOfAllPoolsInEpoch(int epoch);
    public PoolHistory getPoolHistory(String poolId, int epoch);

    public BigInteger getPoolPledgeInEpoch(String poolId, int epoch);

    public PoolOwnerHistory getHistoryOfPoolOwnersInEpoch(String poolId, int epoch);

    public List<PoolDeregistration> getRetiredPoolsInEpoch(int epoch);

    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch);

    public List<MirCertificate> getMirCertificatesInEpoch(int epoch);

    public int getPoolRegistrationsInEpoch(int epoch);

    public List<PoolUpdate> getPoolUpdateAfterTransactionIdInEpoch(String poolId, long transactionId, int epoch);

    public PoolDeregistration latestPoolRetirementUntilEpoch(String poolId, int epoch);

    public BigInteger getTransactionDepositsInEpoch(int epoch);

    public BigInteger getSumOfFeesInEpoch(int epoch);

    public BigInteger getSumOfWithdrawalsInEpoch(int epoch);

    public List<Reward> getRewardListForPoolInEpoch(int epoch, String poolId);

    public BigInteger getTotalPoolRewardsInEpoch(String poolId, int epoch);

    public List<String> getPoolsThatProducedBlocksInEpoch(int epoch);
    public List<LatestStakeAccountUpdate> getLatestStakeAccountUpdates(int epoch, List<String> stakeAddresses);
}
