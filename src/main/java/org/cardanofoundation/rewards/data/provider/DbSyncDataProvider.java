package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.calculation.PoolRewardCalculation;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.entity.jpa.*;
import org.cardanofoundation.rewards.entity.jpa.projection.PoolEpochStake;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;
import org.cardanofoundation.rewards.enums.MirPot;
import org.cardanofoundation.rewards.mapper.*;
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
    DbSyncEpochStakeRepository dbSyncEpochStakeRepository;

    @Autowired
    DbSyncPoolUpdateRepository dbSyncPoolUpdateRepository;

    @Autowired
    DbSyncPoolOwnerRepository dbSyncPoolOwnerRepository;

    @Autowired
    DbSyncRewardRepository dbSyncRewardRepository;

    @Autowired
    DbSyncPoolRetirementRepository dbSyncPoolRetirementRepository;

    @Autowired
    DbSyncStakeDeregistrationRepository dbSyncStakeDeregistrationRepository;

    @Autowired
    DbSyncStakeRegistrationRepository dbSyncStakeRegistrationRepository;

    @Autowired
    DbSyncTransactionRepository dbSyncTransactionRepository;

    @Override
    public AdaPots getAdaPotsForEpoch(int epoch) {
        DbSyncAdaPots dbSyncAdaPots = dbSyncAdaPotsRepository.findByEpoch(epoch);
        return AdaPotsMapper.fromDbSyncAdaPots(dbSyncAdaPots);
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        DbSyncEpoch dbSyncEpoch = dbSyncEpochRepository.findByNumber(epoch);
        Epoch epochInfo = EpochMapper.fromDbSyncEpoch(dbSyncEpoch);

        if (epoch < 211) {
            epochInfo.setOBFTBlockCount(epochInfo.getBlockCount());
            epochInfo.setNonOBFTBlockCount(0);
        } else if (epoch > 256) {
            epochInfo.setOBFTBlockCount(0);
            epochInfo.setNonOBFTBlockCount(epochInfo.getBlockCount());
        } else {
            Integer nonObftBlocks = dbSyncBlockRepository.getNonOBFTBlocksInEpoch(epoch);
            Integer obftBlocks = dbSyncBlockRepository.getOBFTBlocksInEpoch(epoch);
            epochInfo.setOBFTBlockCount(obftBlocks);
            epochInfo.setNonOBFTBlockCount(nonObftBlocks);
        }

        Long epochStake = dbSyncEpochStakeRepository.getEpochStakeByEpoch(epoch);
        epochInfo.setActiveStake(epochStake.doubleValue());

        return epochInfo;
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        DbSyncProtocolParameters dbSyncProtocolParameters = dbSyncProtocolParametersRepository.findByEpoch(epoch);
        return ProtocolParametersMapper.fromDbSyncProtocolParameters(dbSyncProtocolParameters);
    }

    @Override
    public PoolHistory getPoolHistory(String poolId, int epoch) {
        PoolHistory poolHistory = new PoolHistory();
        List<PoolEpochStake> poolEpochStakes = dbSyncEpochStakeRepository.getPoolActiveStakeInEpoch(poolId, epoch);
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

        if (delegators.isEmpty()) {
            return null;
        }

        poolHistory.setActiveStake(activeStake);
        poolHistory.setDelegators(delegators);

        Integer blockCount = dbSyncBlockRepository.getBlocksMadeByPoolInEpoch(poolId, epoch);
        poolHistory.setBlockCount(blockCount);

        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestUpdateForEpoch(poolId, epoch);
        poolHistory.setFixedCost(dbSyncPoolUpdate.getFixedCost());
        poolHistory.setMargin(dbSyncPoolUpdate.getMargin());
        poolHistory.setEpoch(epoch);

        double totalPoolRewards = dbSyncRewardRepository.getRewardsForPoolInEpoch(poolId, epoch);

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

        List<PoolEpochStake> poolEpochStakes = dbSyncEpochStakeRepository.getPoolActiveStakeInEpoch(poolId, epoch);
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
        List<DbSyncPoolRetirement> dbSyncPoolRetirements = dbSyncPoolRetirementRepository.getPoolRetirementsByEpoch(epoch);
        return dbSyncPoolRetirements.stream().map(PoolDeregistrationMapper::fromDbSyncPoolRetirement).toList();
    }

    @Override
    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch) {
        List<AccountUpdate> accountUpdates = new ArrayList<>();

        List<DbSyncAccountDeregistration> stakeDeregistrationsInEpoch =
                dbSyncStakeDeregistrationRepository.getLatestAccountDeregistrationsUntilEpochForAddresses(stakeAddresses, epoch);
        List<DbSyncAccountRegistration> stakeRegistrationsInEpoch =
                dbSyncStakeRegistrationRepository.getLatestAccountRegistrationsUntilEpochForAddresses(stakeAddresses, epoch);

        for (DbSyncAccountDeregistration deregistration : stakeDeregistrationsInEpoch) {
            accountUpdates.add(AccountUpdate.builder()
                    .epoch(deregistration.getEpoch())
                    .action(AccountUpdateAction.DEREGISTRATION)
                    .stakeAddress(deregistration.getAddress().getView())
                    .unixBlockTime(deregistration.getTransaction().getBlock().getTime().getTime())
                    .build());
        }

        for (DbSyncAccountRegistration registration : stakeRegistrationsInEpoch) {
            accountUpdates.add(AccountUpdate.builder()
                    .epoch(registration.getEpoch())
                    .action(AccountUpdateAction.REGISTRATION)
                    .stakeAddress(registration.getAddress().getView())
                    .unixBlockTime(registration.getTransaction().getBlock().getTime().getTime())
                    .build());
        }

        return  accountUpdates;
    }

    @Override
    public List<MirCertificate> getMirCertificatesInEpoch(final int epoch) {
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

    @Override
    public int getAccountRegistrationsInEpoch(int epoch) {
        return dbSyncStakeRegistrationRepository.countRegistrationsInEpoch(epoch);
    }

    @Override
    public int getAccountDeregistrationsInEpoch(int epoch) {
        return dbSyncStakeDeregistrationRepository.countDeregistrationsInEpoch(epoch);
    }

    @Override
    public int getPoolRegistrationsInEpoch(int epoch) {
        return dbSyncPoolUpdateRepository.countPoolRegistrationsInEpoch(epoch);
    }

    @Override
    public List<PoolUpdate> getPoolUpdateAfterTransactionIdInEpoch(String poolId, long transactionId, int epoch) {
        return dbSyncPoolUpdateRepository.findByBech32PoolIdAfterTransactionIdInEpoch(poolId, transactionId, epoch).stream()
                .map(PoolUpdateMapper::fromDbSyncPoolUpdate).toList();
    }

    @Override
    public int getPoolDeregistrationsInEpoch(int epoch) {
        return 0;
    }

    @Override
    public Double getTransactionDepositsInEpoch(int epoch) {
        return dbSyncTransactionRepository.getSumOfDepositsInEpoch(epoch);
    }

    public List<String> getPoolsThatProducedBlocksInEpoch(int epoch) {
        return dbSyncBlockRepository.getPoolsThatProducedBlocksInEpoch(epoch);
    }
}
