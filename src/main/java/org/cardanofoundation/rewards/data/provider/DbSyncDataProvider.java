package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.entity.jpa.DbSyncAdaPots;
import org.cardanofoundation.rewards.entity.jpa.DbSyncEpoch;
import org.cardanofoundation.rewards.entity.jpa.DbSyncProtocolParameters;
import org.cardanofoundation.rewards.entity.jpa.projection.PoolEpochStake;
import org.cardanofoundation.rewards.mapper.AdaPotsMapper;
import org.cardanofoundation.rewards.mapper.EpochMapper;
import org.cardanofoundation.rewards.mapper.ProtocolParametersMapper;
import org.cardanofoundation.rewards.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Profile("db-sync")
public class DbSyncDataProvider implements DataProvider {

    @Autowired
    DbSyncEpochRepository dbSyncEpochRepository;
    @Autowired
    DbSyncAdaPotsRepository dbSyncAdaPotsRepository;
    @Autowired
    DbSyncProtocolParametersRepository dbSyncProtocolParametersRepository;
    @Autowired
    DbSyncBlockRepository dbSyncBlockRepository;
    @Autowired
    DbSyncEpochStakeRepository dbSyncPoolHistoryRepository;

    @Override
    public AdaPots getAdaPotsForEpoch(int epoch) {
        DbSyncAdaPots dbSyncAdaPots = dbSyncAdaPotsRepository.findByEpoch(epoch);
        return AdaPotsMapper.fromDbSyncAdaPots(dbSyncAdaPots);
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        DbSyncEpoch dbSyncEpoch = dbSyncEpochRepository.findByNumber(epoch);
        return EpochMapper.fromDbSyncEpoch(dbSyncEpoch);
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        DbSyncProtocolParameters dbSyncProtocolParameters = dbSyncProtocolParametersRepository.findByEpoch(epoch);
        return ProtocolParametersMapper.fromDbSyncProtocolParameters(dbSyncProtocolParameters);
    }

    @Override
    public PoolHistory getPoolHistory(String poolId, int epoch) {
        PoolHistory poolHistory = new PoolHistory();
        List<PoolEpochStake> poolEpochStakes = dbSyncPoolHistoryRepository.getPoolActiveStakeInEpoch(poolId, epoch);
        double activeStake = 0;
        List<Delegator> delegators = new ArrayList<>();
        for (PoolEpochStake poolEpochStake : poolEpochStakes) {
            activeStake += poolEpochStake.getAmount();
            Delegator delegator = Delegator.builder()
                    .stakeAddress(poolEpochStake.getStakeAddress())
                    .activeStake(poolEpochStake.getAmount())
                    .build();
            delegators.add(delegator);
        }
        poolHistory.setActiveStake(activeStake);
        poolHistory.setDelegators(delegators);

        Integer blockCount = dbSyncBlockRepository.getBlocksMadeByPoolInEpoch(poolId, epoch);
        poolHistory.setBlockCount(blockCount);
        return poolHistory;
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
