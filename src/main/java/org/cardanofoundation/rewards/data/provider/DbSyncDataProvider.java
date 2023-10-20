package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.entity.jpa.DbSyncEpoch;
import org.cardanofoundation.rewards.mapper.EpochMapper;
import org.cardanofoundation.rewards.repository.DbSyncEpochRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Profile("db-sync")
public class DbSyncDataProvider implements DataProvider {

    @Autowired
    DbSyncEpochRepository dbSyncEpochRepository;

    @Override
    public AdaPots getAdaPotsForEpoch(int epoch) {
        return null;
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        DbSyncEpoch dbSyncEpoch = dbSyncEpochRepository.findByNumber(epoch);
        return EpochMapper.fromDbSyncEpoch(dbSyncEpoch);
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        return null;
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
    public PoolOwnerHistory getHistoryOfPoolOwnersInEpoch(String poolId, int epoch) {
        return null;
    }

    @Override
    public List<PoolDeregistration> getRetiredPoolsInEpoch(int epoch) {
        return null;
    }

    @Override
    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch) {
        return null;
    }

    @Override
    public List<MirCertificate> getMirCertificatesInEpoch(int epoch) {
        return null;
    }
}
