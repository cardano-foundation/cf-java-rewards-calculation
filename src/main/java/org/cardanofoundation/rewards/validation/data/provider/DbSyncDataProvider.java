package org.cardanofoundation.rewards.validation.data.provider;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.entity.jpa.*;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.*;
import org.cardanofoundation.rewards.calculation.enums.AccountUpdateAction;
import org.cardanofoundation.rewards.calculation.enums.MirPot;
import org.cardanofoundation.rewards.validation.mapper.*;
import org.cardanofoundation.rewards.validation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.EXPECTED_SLOT_PER_EPOCH;

@Service
@Slf4j
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

    @Autowired
    DbSyncWithdrawalRepository dbSyncWithdrawalRepository;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUser;

    @Value("${spring.datasource.password}")
    private String databasePassword;

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

        BigInteger epochStake = dbSyncEpochStakeRepository.getEpochStakeByEpoch(epoch);
        epochInfo.setActiveStake(epochStake);

        return epochInfo;
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        DbSyncProtocolParameters dbSyncProtocolParameters = dbSyncProtocolParametersRepository.findByEpoch(epoch);
        return ProtocolParametersMapper.fromDbSyncProtocolParameters(dbSyncProtocolParameters);
    }

    public List<PoolHistory> fetchPoolHistoryInBatches(Integer epoch, int batchSize, List<PoolBlocks> blocksMadeByPoolsInEpoch) {
        List<PoolHistory> poolHistories = new ArrayList<>();
        List<String> poolIds = blocksMadeByPoolsInEpoch.stream()
                .map(PoolBlocks::getPoolId)
                .distinct()
                .toList();

        List<LatestPoolUpdate> latestUpdates = dbSyncPoolUpdateRepository.findLatestActiveUpdatesInEpoch(epoch, poolIds);

        List<Long> updateIds = latestUpdates.stream()
                .map(LatestPoolUpdate::getId)
                .toList();

        List<PoolOwner> owners = dbSyncPoolOwnerRepository.getOwnersByPoolUpdateIds(updateIds);
        List<List<String>> poolIdBatches = new ArrayList<>();

        for (int i = 0; i < poolIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, poolIds.size());
            List<String> batch = poolIds.subList(i, end);
            poolIdBatches.add(batch);
        }

        int i = 0;
        int batches = poolIdBatches.size();
        for (List<String> poolIdBatch : poolIdBatches) {
            log.info("fetching pool history batch " + i + " / " + batches + " for epoch " + epoch + " with " + poolIdBatch.size() + " pools");
            List<PoolEpochStake> poolEpochStakes = dbSyncEpochStakeRepository.getAllPoolsActiveStakesInEpoch(epoch, poolIdBatch);
            List<Delegator> delegators = poolEpochStakes.stream()
                    .map(DelegatorMapper::fromPoolEpochStake)
                    .toList();

            for (String poolId : poolIdBatch) {
                PoolHistory poolHistory = new PoolHistory();

                if (!delegators.isEmpty()) {
                    List<Delegator> poolDelegators = delegators.stream()
                            .filter(delegator -> delegator.getPoolId().equals(poolId))
                            .toList();
                    BigInteger activeStake = poolDelegators.stream()
                            .map(Delegator::getActiveStake)
                            .reduce(BigInteger::add)
                            .orElse(BigInteger.ZERO);

                    poolHistory.setActiveStake(activeStake);
                    poolHistory.setDelegators(poolDelegators);

                    Integer blockCount = blocksMadeByPoolsInEpoch.stream()
                            .filter(poolBlocks -> poolBlocks.getPoolId().equals(poolId))
                            .map(PoolBlocks::getBlockCount)
                            .findFirst()
                            .orElse(0);
                    poolHistory.setBlockCount(blockCount);

                    LatestPoolUpdate latestUpdate = latestUpdates.stream()
                            .filter(update -> update.getPoolId().equals(poolId))
                            .findFirst()
                            .orElse(null);

                    if (latestUpdate == null) {
                        System.out.println("No update for pool " + poolId + " in epoch " + epoch);
                        continue;
                    }

                    poolHistory.setFixedCost(latestUpdate.getFixedCost());
                    poolHistory.setMargin(latestUpdate.getMargin());
                    poolHistory.setRewardAddress(latestUpdate.getRewardAddress());
                    poolHistory.setPledge(latestUpdate.getPledge());
                    poolHistory.setEpoch(epoch);
                    poolHistory.setPoolId(poolId);


                    List<String> poolOwnerStakeAddresses = owners.stream()
                            .filter(owner -> owner.getPoolId().equals(poolId)).map(PoolOwner::getStakeAddress).toList();

                    BigInteger poolOwnerActiveStake = BigInteger.ZERO;
                    for (Delegator poolDelegator : poolDelegators) {
                        if (poolOwnerStakeAddresses.contains(poolDelegator.getStakeAddress())) {
                            poolOwnerActiveStake = poolOwnerActiveStake.add(poolDelegator.getActiveStake());
                        }
                    }

                    poolHistory.setOwners(poolOwnerStakeAddresses);
                    poolHistory.setOwnerActiveStake(poolOwnerActiveStake);

                    poolHistories.add(poolHistory);
                }
            }
            i++;
        }

        return poolHistories;
    }

    @Override
    public List<PoolHistory> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlocks> blocksMadeByPoolsInEpoch) {
        return fetchPoolHistoryInBatches(epoch, 20, blocksMadeByPoolsInEpoch);
    }

    @Override
    public PoolHistory getPoolHistory(String poolId, int epoch) {
        PoolHistory poolHistory = new PoolHistory();
        List<PoolEpochStake> poolEpochStakes = dbSyncEpochStakeRepository.getPoolActiveStakeInEpoch(poolId, epoch);
        BigInteger activeStake = BigInteger.ZERO;
        List<Delegator> delegators = new ArrayList<>();
        for (PoolEpochStake poolEpochStake : poolEpochStakes) {
            activeStake = activeStake.add(poolEpochStake.getAmount());
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

        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestActiveUpdateInEpoch(poolId, epoch);
        poolHistory.setFixedCost(dbSyncPoolUpdate.getFixedCost());
        poolHistory.setMargin(dbSyncPoolUpdate.getMargin());
        poolHistory.setPledge(dbSyncPoolUpdate.getPledge());
        poolHistory.setEpoch(epoch);
        poolHistory.setPoolId(poolId);
        poolHistory.setRewardAddress(dbSyncPoolUpdate.getStakeAddress().getView());

        List<PoolOwner> owners = dbSyncPoolOwnerRepository.getOwnersByPoolUpdateIds(List.of(dbSyncPoolUpdate.getId()));
        BigInteger activeStakes = BigInteger.ZERO;
        List<String> stakeAddresses = owners.stream()
                .filter(owner -> owner.getPoolId().equals(poolId)).map(PoolOwner::getStakeAddress).toList();

        for (PoolEpochStake poolEpochStake : poolEpochStakes) {
            if (stakeAddresses.contains(poolEpochStake.getStakeAddress())) {
                activeStakes = activeStakes.add(poolEpochStake.getAmount());
            }
        }

        poolHistory.setOwners(stakeAddresses);
        poolHistory.setOwnerActiveStake(activeStakes);

        return poolHistory;
    }

    @Override
    public BigInteger getPoolPledgeInEpoch(String poolId, int epoch) {
        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestActiveUpdateInEpoch(poolId, epoch);

        if (dbSyncPoolUpdate == null) {
            return null;
        }

        return dbSyncPoolUpdate.getPledge();
    }

    @Override
    public PoolOwnerHistory getHistoryOfPoolOwnersInEpoch(String poolId, int epoch) {
        PoolOwnerHistory poolOwnerHistory = new PoolOwnerHistory();
        poolOwnerHistory.setEpoch(epoch);

        List<PoolEpochStake> poolEpochStakes = dbSyncEpochStakeRepository.getPoolActiveStakeInEpoch(poolId, epoch);
        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestActiveUpdateInEpoch(poolId, epoch);

        if (dbSyncPoolUpdate == null) {
            return null;
        }

        List<DbSyncPoolOwner> owners = dbSyncPoolOwnerRepository.getByPoolUpdateId(dbSyncPoolUpdate.getId());

        List<String> stakeAddresses = owners.stream()
                .map(DbSyncPoolOwner::getStakeAddress)
                .map(DbSyncStakeAddress::getView)
                .toList();

        BigInteger activeStakes = BigInteger.ZERO;

        for (PoolEpochStake poolEpochStake : poolEpochStakes) {
            if (stakeAddresses.contains(poolEpochStake.getStakeAddress())) {
                activeStakes = activeStakes.add(poolEpochStake.getAmount());
            }
        }
        poolOwnerHistory.setStakeAddresses(stakeAddresses);
        poolOwnerHistory.setActiveStake(activeStakes);
        return poolOwnerHistory;
    }

    @Override
    public List<PoolDeregistration> getRetiredPoolsInEpoch(int epoch) {
        List<DbSyncPoolRetirement> dbSyncPoolRetirements = dbSyncPoolRetirementRepository.getPoolRetirementsByEpoch(epoch);
        List<PoolDeregistration> poolDeregistrations = dbSyncPoolRetirements.stream().map(PoolDeregistrationMapper::fromDbSyncPoolRetirement).toList();
        List<PoolDeregistration> retiredPools = new ArrayList<>();

        for (PoolDeregistration poolDeregistration : poolDeregistrations) {
            boolean poolDeregistrationLaterInEpoch = poolDeregistrations.stream().anyMatch(
                    deregistration -> deregistration.getPoolId().equals(poolDeregistration.getPoolId()) &&
                            deregistration.getAnnouncedTransactionId() > poolDeregistration.getAnnouncedTransactionId()
            );

            // To prevent double counting, we only count the pool deregistration if there is no other deregistration
            // for the same pool later in the epoch
            if (poolDeregistrationLaterInEpoch) {
                continue;
            }

            List<PoolUpdate> poolUpdates = this.getPoolUpdateAfterTransactionIdInEpoch(poolDeregistration.getPoolId(),
                    poolDeregistration.getAnnouncedTransactionId(), epoch - 1);

            // There is an update after the deregistration, so the pool has not been retired
            if (poolUpdates.size() == 0) {
                PoolDeregistration latestPoolRetirementUntilEpoch = this.latestPoolRetirementUntilEpoch(poolDeregistration.getPoolId(), epoch - 1);
                if (latestPoolRetirementUntilEpoch != null && latestPoolRetirementUntilEpoch.getRetiringEpoch() != epoch) {
                    // The pool was retired in a previous epoch for the next epoch, but another deregistration was announced and changed the
                    // retirement epoch to something else. This means the pool was not retired in this epoch.
                    continue;
                }

                DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLatestUpdateInEpoch(poolDeregistration.getPoolId(), epoch);
                poolDeregistration.setRewardAddress(dbSyncPoolUpdate.getStakeAddress().getView());
                retiredPools.add(poolDeregistration);
            }
        }

        return retiredPools;
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
    public int getPoolRegistrationsInEpoch(int epoch) {
        return dbSyncPoolUpdateRepository.countPoolRegistrationsInEpoch(epoch);
    }

    @Override
    public List<PoolUpdate> getPoolUpdateAfterTransactionIdInEpoch(String poolId, long transactionId, int epoch) {
        return dbSyncPoolUpdateRepository.findByBech32PoolIdAfterTransactionIdInEpoch(poolId, transactionId, epoch).stream()
                .map(PoolUpdateMapper::fromDbSyncPoolUpdate).toList();
    }

    @Override
    public PoolDeregistration latestPoolRetirementUntilEpoch(String poolId, int epoch) {
        DbSyncPoolRetirement dbSyncPoolRetirement = dbSyncPoolRetirementRepository.latestPoolRetirementUntilEpoch(poolId, epoch);
        return PoolDeregistrationMapper.fromDbSyncPoolRetirement(dbSyncPoolRetirement);
    }

    @Override
    public BigInteger getTransactionDepositsInEpoch(int epoch) {
        return dbSyncTransactionRepository.getSumOfDepositsInEpoch(epoch);
    }

    @Override
    public BigInteger getSumOfFeesInEpoch(int epoch) {
        return dbSyncTransactionRepository.getSumOfFeesInEpoch(epoch);
    }

    @Override
    public BigInteger getSumOfWithdrawalsInEpoch(int epoch) {
        return dbSyncWithdrawalRepository.getSumOfWithdrawalsInEpoch(epoch);
    }

    @Override
    public List<Reward> getMemberRewardsInEpoch(int epoch) {
        List <MemberReward> poolRewards = dbSyncRewardRepository.getMemberRewardsInEpoch(epoch);

        if (poolRewards.isEmpty()) {
            return List.of();
        }

        return poolRewards.stream().map(RewardMapper::fromMemberReward).toList();
    }

    @Override
    public List<TotalPoolRewards> getSumOfMemberAndLeaderRewardsInEpoch(int epoch) {
        List<TotalPoolRewards> totalPoolRewards = dbSyncRewardRepository.getSumOfMemberAndLeaderRewardsInEpoch(epoch);

        if (totalPoolRewards.isEmpty()) {
            return List.of();
        } else {
            return totalPoolRewards;
        }
    }

    @Override
    public BigInteger getTotalPoolRewardsInEpoch(String poolId, int epoch) {
        return dbSyncRewardRepository.getTotalPoolRewardsInEpoch(poolId, epoch);
    }

    public List<PoolBlocks> getBlocksMadeByPoolsInEpoch(int epoch) {
        return dbSyncBlockRepository.getAllBlocksMadeByPoolsInEpoch(epoch);
    }

    @Override
    public List<AccountUpdate> getLatestStakeAccountUpdates(int epoch) {
        List<LatestStakeAccountUpdate> latestStakeAccountUpdates = dbSyncStakeDeregistrationRepository.getLatestStakeAccountUpdates(epoch);
        return latestStakeAccountUpdates.stream().map(AccountUpdateMapper::fromLatestStakeAccountUpdate).toList();
    }

    @Override
    public List<String> findSharedPoolRewardAddressWithoutReward(int epoch) {
        return dbSyncPoolUpdateRepository.findSharedPoolRewardAddressWithoutReward(epoch);
    }

    @Override
    public List<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow) {
        return dbSyncStakeDeregistrationRepository.getAccountDeregistrationsInEpoch(epoch, stabilityWindow);
    }

    @Override
    public List<String> getLateAccountDeregistrationsInEpoch(int epoch, long stabilityWindow) {
        return dbSyncStakeDeregistrationRepository.getLateAccountDeregistrationsInEpoch(epoch,  stabilityWindow);
    }

    @Override
    public List<String> getStakeAddressesWithRegistrationsUntilEpoch(Integer epoch, List<String> stakeAddresses, Long stabilityWindow) {
        return dbSyncStakeRegistrationRepository.getStakeAddressesWithRegistrationsUntilEpoch(epoch, stakeAddresses, stabilityWindow);
    }
}
