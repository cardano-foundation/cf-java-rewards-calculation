package org.cardanofoundation.rewards.validation.data.provider;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;

public interface DataProvider {

    public AdaPots getAdaPotsForEpoch(int epoch);

    public Epoch getEpochInfo(int epoch);

    public ProtocolParameters getProtocolParametersForEpoch(int epoch);

    public List<PoolHistory> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch);
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

    public HashSet<Reward> getMemberRewardsInEpoch(int epoch);

    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch);
    public HashSet<AccountUpdate> getLatestStakeAccountUpdates(int epoch, HashSet<String> accounts);
    public HashSet<PoolReward> getTotalPoolRewardsInEpoch(int epoch);

    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch);

    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow);
    public HashSet<String> getLateAccountDeregistrationsInEpoch(int epoch, long stabilityWindow);
    public HashSet<String> getRegisteredAccountsUntilLastEpoch(Integer epoch, HashSet<String> stakeAddresses,
                                                               Long stabilityWindow);

    HashSet<String> getRegisteredAccountsUntilNow(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow);
}
