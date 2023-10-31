package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.calculation.PoolRewardCalculation;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.entity.jpa.*;
import org.cardanofoundation.rewards.entity.jpa.projection.PoolEpochStake;
import org.cardanofoundation.rewards.enums.MirPot;
import org.cardanofoundation.rewards.mapper.AdaPotsMapper;
import org.cardanofoundation.rewards.mapper.EpochMapper;
import org.cardanofoundation.rewards.mapper.ProtocolParametersMapper;
import org.cardanofoundation.rewards.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;

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

    @Autowired
    DbSyncPoolUpdateRepository dbSyncPoolUpdateRepository;

    @Autowired
    DbSyncPoolOwnerRepository dbSyncPoolOwnerRepository;

    @Autowired
    DbSyncRewardRepository dbSyncRewardRepository;

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

        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestUpdateForEpoch(poolId, epoch);
        poolHistory.setFixedCost(dbSyncPoolUpdate.getFixedCost());
        poolHistory.setMargin(dbSyncPoolUpdate.getMargin());
        poolHistory.setEpoch(epoch);

        List<DbSyncReward> rewards = dbSyncRewardRepository.getRewardsForPoolInEpoch(poolId, epoch);
        double totalPoolRewards = 0;
        for (DbSyncReward reward : rewards) {
            totalPoolRewards += reward.getAmount();
        }

        DbSyncAdaPots dbSyncAdaPots = dbSyncAdaPotsRepository.findByEpoch(epoch + 1);
        double reserves = dbSyncAdaPots.getReserves();
        double adaInCirculation = TOTAL_LOVELACE - reserves;
        double relativePoolStake = activeStake / adaInCirculation;

        List<DbSyncPoolOwner> owners = dbSyncPoolOwnerRepository.getByPoolUpdateId(dbSyncPoolUpdate.getId());

        double totalActiveStakeOfOwners = 0.0;
        double totalMemberRewardsOfOwners = 0.0;

        for (DbSyncPoolOwner owner : owners) {
            Delegator delegator = poolHistory.getDelegator(owner.getStakeAddress().getView());
            totalActiveStakeOfOwners += delegator.getActiveStake();
            totalMemberRewardsOfOwners += PoolRewardCalculation.calculateMemberReward(totalPoolRewards,
                    poolHistory.getMargin(), poolHistory.getFixedCost(),
                    delegator.getActiveStake() / adaInCirculation, relativePoolStake);

        }

        double poolOperatorReward = PoolRewardCalculation.calculateLeaderReward(totalPoolRewards,
                poolHistory.getMargin(), poolHistory.getFixedCost(),
                totalActiveStakeOfOwners / adaInCirculation, relativePoolStake);

        poolHistory.setPoolFees(poolOperatorReward - totalMemberRewardsOfOwners);
        poolHistory.setDelegatorRewards(totalPoolRewards - poolOperatorReward + totalMemberRewardsOfOwners);

        return poolHistory;
    }

    @Override
    public Double getPoolPledgeInEpoch(String poolId, int epoch) {
        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestUpdateForEpoch(poolId, epoch);
        return dbSyncPoolUpdate.getPledge();
    }

    @Override
    public PoolOwnerHistory getHistoryOfPoolOwnersInEpoch(String poolId, int epoch) {
        PoolOwnerHistory poolOwnerHistory = new PoolOwnerHistory();
        poolOwnerHistory.setEpoch(epoch);

        List<PoolEpochStake> poolEpochStakes = dbSyncPoolHistoryRepository.getPoolActiveStakeInEpoch(poolId, epoch);
        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestUpdateForEpoch(poolId, epoch);
        List<DbSyncPoolOwner> owners = dbSyncPoolOwnerRepository.getByPoolUpdateId(dbSyncPoolUpdate.getId());

        List<String> stakeAddresses = owners.stream()
                .map(DbSyncPoolOwner::getStakeAddress)
                .map(DbSyncStakeAddress::getView)
                .toList();

        double activeStakes = 0.0;

        for (PoolEpochStake poolEpochStake : poolEpochStakes) {
            if (stakeAddresses.contains(poolEpochStake.getStakeAddress())) {
                activeStakes += poolEpochStake.getAmount();
            }
        }
        poolOwnerHistory.setStakeAddresses(stakeAddresses);
        poolOwnerHistory.setActiveStake(activeStakes);
        return poolOwnerHistory;
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
        List<DbSyncReward> mirCertificatesInEpoch = dbSyncRewardRepository.getMIRCertificatesInEpoch(epoch);
        List<MirCertificate> mirCertificates = new ArrayList<>();

        for (DbSyncReward dbSyncReward : mirCertificatesInEpoch) {
            MirCertificate mirCertificate = new MirCertificate();
            mirCertificate.setPot(MirPot.valueOf(dbSyncReward.getType().toUpperCase()));
            mirCertificate.setTotalRewards(dbSyncReward.getAmount());
            mirCertificates.add(mirCertificate);
        }

        return mirCertificates;
    }
}
