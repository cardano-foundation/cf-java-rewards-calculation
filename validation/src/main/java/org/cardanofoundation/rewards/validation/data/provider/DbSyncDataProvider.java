package org.cardanofoundation.rewards.validation.data.provider;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.calculation.domain.PoolBlock;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.calculation.enums.MirPot;
import org.cardanofoundation.rewards.validation.entity.dbsync.*;
import org.cardanofoundation.rewards.validation.entity.projection.*;
import org.cardanofoundation.rewards.validation.mapper.*;
import org.cardanofoundation.rewards.validation.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.MAINNET_SHELLEY_START_EPOCH;

@Service
@Slf4j
@Profile("db-sync")
public class DbSyncDataProvider implements DataProvider {

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

    @Override
    public AdaPots getAdaPotsForEpoch(int epoch) {
        DbSyncAdaPots dbSyncAdaPots = dbSyncAdaPotsRepository.findByEpoch(epoch);
        return AdaPotsMapper.fromDbSyncAdaPots(dbSyncAdaPots);
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        if (epoch < MAINNET_SHELLEY_START_EPOCH) {
            return null;
        }

        // We need to fetch the block count and fees in this way
        // and not using the aggregations from the epoch table, to
        // avoid running in https://github.com/IntersectMBO/cardano-db-sync/issues/1457
        // for old instances.
        Integer blockCount = dbSyncBlockRepository.countByEpochNo(epoch);
        BigInteger fees = this.getSumOfFeesInEpoch(epoch);

        Epoch epochInfo = Epoch.builder()
                .number(epoch)
                .fees(fees)
                .blockCount(blockCount)
                .build();

        if (epoch < 211) {
            epochInfo.setNonOBFTBlockCount(0);
        } else if (epoch > 256) {
            epochInfo.setNonOBFTBlockCount(epochInfo.getBlockCount());
        } else {
            Integer nonObftBlocks = dbSyncBlockRepository.getNonOBFTBlocksInEpoch(epoch);
            epochInfo.setNonOBFTBlockCount(nonObftBlocks);
        }

        BigInteger epochStake = dbSyncEpochStakeRepository.getEpochStakeByEpoch(epoch);
        epochInfo.setFees(fees);
        epochInfo.setActiveStake(epochStake);

        return epochInfo;
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        DbSyncProtocolParameters dbSyncProtocolParameters = dbSyncProtocolParametersRepository.findByEpoch(epoch);
        return ProtocolParametersMapper.fromDbSyncProtocolParameters(dbSyncProtocolParameters);
    }

    public List<PoolState> fetchPoolHistoryInBatches(Integer epoch, int batchSize, List<PoolBlock> blocksMadeByPoolsInEpoch) {
        List<PoolState> poolHistories = new ArrayList<>();
        List<String> poolIds = blocksMadeByPoolsInEpoch.stream()
                .map(PoolBlock::getPoolId)
                .distinct()
                .toList();

        HashSet<LatestPoolUpdate> latestUpdates = dbSyncPoolUpdateRepository.findLatestActiveUpdatesInEpoch(epoch, poolIds);

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
            HashSet<PoolEpochStake> poolEpochStakes = dbSyncEpochStakeRepository.getAllPoolsActiveStakesInEpoch(epoch, poolIdBatch);

            for (String poolId : poolIdBatch) {
                PoolState poolState = new PoolState();

                HashSet<Delegator> delegators = poolEpochStakes.stream()
                        .filter(epochStake -> epochStake.getPoolId().equals(poolId))
                        .map(DelegatorMapper::fromPoolEpochStake)
                        .collect(Collectors.toCollection(HashSet::new));

                if (!delegators.isEmpty()) {
                    BigInteger activeStake = delegators.stream()
                            .map(Delegator::getActiveStake)
                            .reduce(BigInteger::add)
                            .orElse(BigInteger.ZERO);

                    poolState.setActiveStake(activeStake);
                    poolState.setDelegators(delegators);

                    Integer blockCount = blocksMadeByPoolsInEpoch.stream()
                            .filter(poolBlocks -> poolBlocks.getPoolId().equals(poolId))
                            .map(PoolBlock::getBlockCount)
                            .findFirst()
                            .orElse(0);
                    poolState.setBlockCount(blockCount);

                    LatestPoolUpdate latestUpdate = latestUpdates.stream()
                            .filter(update -> update.getPoolId().equals(poolId))
                            .findFirst()
                            .orElse(null);

                    if (latestUpdate == null) {
                        log.info("No update for pool " + poolId + " in epoch " + epoch);
                        continue;
                    }

                    poolState.setFixedCost(latestUpdate.getFixedCost());
                    poolState.setMargin(latestUpdate.getMargin());
                    poolState.setRewardAddress(latestUpdate.getRewardAddress());
                    poolState.setPledge(latestUpdate.getPledge());
                    poolState.setEpoch(epoch);
                    poolState.setPoolId(poolId);


                    HashSet<String> poolOwnerStakeAddresses = owners.stream()
                            .filter(owner -> owner.getPoolId().equals(poolId)).map(PoolOwner::getStakeAddress).collect(Collectors.toCollection(HashSet::new));

                    BigInteger poolOwnerActiveStake = BigInteger.ZERO;
                    for (Delegator delegator : delegators) {
                        if (poolOwnerStakeAddresses.contains(delegator.getStakeAddress())) {
                            poolOwnerActiveStake = poolOwnerActiveStake.add(delegator.getActiveStake());
                        }
                    }

                    poolState.setOwners(poolOwnerStakeAddresses);
                    poolState.setOwnerActiveStake(poolOwnerActiveStake);

                    poolHistories.add(poolState);
                }
            }
            i++;
        }

        return poolHistories;
    }

    @Override
    public List<PoolState> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch) {
        return fetchPoolHistoryInBatches(epoch, 20, blocksMadeByPoolsInEpoch);
    }

    @Override
    public PoolState getPoolHistory(String poolId, int epoch) {
        PoolState poolState = new PoolState();
        List<PoolEpochStake> poolEpochStakes = dbSyncEpochStakeRepository.getPoolActiveStakeInEpoch(poolId, epoch);
        BigInteger activeStake = BigInteger.ZERO;
        HashSet<Delegator> delegators = new HashSet<>();
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

        poolState.setActiveStake(activeStake);
        poolState.setDelegators(delegators);

        Integer blockCount = dbSyncBlockRepository.getBlocksMadeByPoolInEpoch(poolId, epoch);
        poolState.setBlockCount(blockCount);

        DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLastestActiveUpdateInEpoch(poolId, epoch);
        poolState.setFixedCost(dbSyncPoolUpdate.getFixedCost());
        poolState.setMargin(dbSyncPoolUpdate.getMargin());
        poolState.setPledge(dbSyncPoolUpdate.getPledge());
        poolState.setEpoch(epoch);
        poolState.setPoolId(poolId);
        poolState.setRewardAddress(dbSyncPoolUpdate.getStakeAddress().getView());

        List<PoolOwner> owners = dbSyncPoolOwnerRepository.getOwnersByPoolUpdateIds(List.of(dbSyncPoolUpdate.getId()));
        BigInteger activeStakes = BigInteger.ZERO;
        HashSet<String> stakeAddresses = owners.stream()
                .filter(owner -> owner.getPoolId().equals(poolId)).map(PoolOwner::getStakeAddress).collect(Collectors.toCollection(HashSet::new));

        for (PoolEpochStake poolEpochStake : poolEpochStakes) {
            if (stakeAddresses.contains(poolEpochStake.getStakeAddress())) {
                activeStakes = activeStakes.add(poolEpochStake.getAmount());
            }
        }

        poolState.setOwners(stakeAddresses);
        poolState.setOwnerActiveStake(activeStakes);

        return poolState;
    }

    @Override
    public HashSet<String> getRewardAddressesOfRetiredPoolsInEpoch(int epoch) {
        List<DbSyncPoolRetirement> poolDeregistrations = dbSyncPoolRetirementRepository.getPoolRetirementsByEpoch(epoch);
        HashSet<String> rewardAddressesOfRetiredPools = new HashSet<>();

        for (DbSyncPoolRetirement poolDeregistration : poolDeregistrations) {
            boolean poolDeregistrationLaterInEpoch = poolDeregistrations.stream().anyMatch(
                    deregistration -> deregistration.getPool().getBech32PoolId().equals(poolDeregistration.getPool().getBech32PoolId()) &&
                            deregistration.getAnnouncedTransaction().getId() > poolDeregistration.getAnnouncedTransaction().getId()
            );

            // To prevent double counting, we only count the pool deregistration if there is no other deregistration
            // for the same pool later in the epoch
            if (poolDeregistrationLaterInEpoch) {
                continue;
            }

            List<PoolUpdate> poolUpdates = this.getPoolUpdateAfterTransactionIdInEpoch(poolDeregistration.getPool().getBech32PoolId(),
                    poolDeregistration.getAnnouncedTransaction().getId(), epoch - 1);

            // There is an update after the deregistration, so the pool has not been retired
            if (poolUpdates.size() == 0) {
                DbSyncPoolRetirement latestPoolRetirementUntilEpoch = dbSyncPoolRetirementRepository.latestPoolRetirementUntilEpoch(poolDeregistration.getPool().getBech32PoolId(), epoch - 1);
                if (latestPoolRetirementUntilEpoch != null && latestPoolRetirementUntilEpoch.getRetiringEpoch() != epoch) {
                    // The pool was retired in a previous epoch for the next epoch, but another deregistration was announced and changed the
                    // retirement epoch to something else. This means the pool was not retired in this epoch.
                    continue;
                }

                DbSyncPoolUpdate dbSyncPoolUpdate = dbSyncPoolUpdateRepository.findLatestUpdateInEpoch(poolDeregistration.getPool().getBech32PoolId(), epoch);
                rewardAddressesOfRetiredPools.add(dbSyncPoolUpdate.getStakeAddress().getView());
            }
        }

        return rewardAddressesOfRetiredPools;
    }

    @Override
    public List<MirCertificate> getMirCertificatesInEpoch(final int epoch) {
        List<MirTransition> mirTransitions = dbSyncRewardRepository.getMIRCertificatesInEpoch(epoch);
        List<MirCertificate> mirCertificates = new ArrayList<>();

        for (MirTransition mirTransition : mirTransitions) {
            MirCertificate mirCertificate = new MirCertificate();
            mirCertificate.setPot(MirPot.valueOf(mirTransition.getPot().toUpperCase()));
            mirCertificate.setTotalRewards(mirTransition.getTotalRewards());
            mirCertificates.add(mirCertificate);
        }

        return mirCertificates;
    }

    public List<PoolUpdate> getPoolUpdateAfterTransactionIdInEpoch(String poolId, long transactionId, int epoch) {
        return dbSyncPoolUpdateRepository.findByBech32PoolIdAfterTransactionIdInEpoch(poolId, transactionId, epoch).stream()
                .map(PoolUpdateMapper::fromDbSyncPoolUpdate).toList();
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
    public HashSet<Reward> getMemberRewardsInEpoch(int epoch) {
        HashSet<MemberReward> poolRewards = dbSyncRewardRepository.getMemberRewardsInEpoch(epoch);

        if (poolRewards.isEmpty()) {
            return new HashSet<>();
        }

        return poolRewards.stream().map(RewardMapper::fromMemberReward).collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public HashSet<PoolReward> getTotalPoolRewardsInEpoch(int epoch) {
        HashSet<TotalPoolRewards> totalPoolRewards = dbSyncRewardRepository.getSumOfMemberAndLeaderRewardsInEpoch(epoch);

        if (totalPoolRewards.isEmpty()) {
            return new HashSet<>();
        } else {
            return totalPoolRewards.stream().map(totalPoolReward -> PoolReward.fromTotalPoolRewards(totalPoolReward, epoch)).collect(Collectors.toCollection(HashSet::new));
        }
    }

    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch) {
        return dbSyncBlockRepository.getAllBlocksMadeByPoolsInEpoch(epoch).stream().map(block -> {
            PoolBlock poolBlock = new PoolBlock();
            poolBlock.setPoolId(block.getPoolId());
            poolBlock.setBlockCount(block.getBlockCount());
            return poolBlock;
        }).toList();
    }

    @Override
    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch) {
        return dbSyncPoolUpdateRepository.findSharedPoolRewardAddressWithoutReward(epoch);
    }

    @Override
    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow) {
        return dbSyncStakeDeregistrationRepository.getAccountDeregistrationsInEpoch(epoch, stabilityWindow);
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilLastEpoch(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        return dbSyncStakeRegistrationRepository.getStakeAddressRegistrationsUntilEpoch(epoch - 1, stakeAddresses, stabilityWindow);
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilNow(Integer epoch, HashSet<String> stakeAddresses, Long stabilisationWindow) {
        return dbSyncStakeRegistrationRepository.getStakeAddressRegistrationsUntilEpoch(epoch, stakeAddresses, stabilisationWindow);
    }
}
