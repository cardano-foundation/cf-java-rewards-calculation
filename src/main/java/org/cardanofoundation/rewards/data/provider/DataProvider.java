package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;

import java.util.List;

public interface DataProvider {

    public AdaPots getAdaPotsForEpoch(int epoch);

    public Epoch getEpochInfo(int epoch);

    public ProtocolParameters getProtocolParametersForEpoch(int epoch);

    public PoolHistory getPoolHistory(String poolId, int epoch);

    public Double getPoolPledgeInEpoch(String poolId, int epoch);

    public PoolOwnerHistory getHistoryOfPoolOwnersInEpoch(String poolId, int epoch);

    public List<PoolDeregistration> getRetiredPoolsInEpoch(int epoch);

    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch);

    public List<MirCertificate> getMirCertificatesInEpoch(int epoch);

    public int getAccountRegistrationsInEpoch(int epoch);

    public int getAccountDeregistrationsInEpoch(int epoch);

    public int getPoolRegistrationsInEpoch(int epoch);

    public int getPoolDeregistrationsInEpoch(int epoch);

    public Double getTransactionDepositsInEpoch(int epoch);
}
