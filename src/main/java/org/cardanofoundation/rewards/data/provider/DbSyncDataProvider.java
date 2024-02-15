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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.BigNumberUtils.divide;

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

    @Autowired
    DbSyncWithdrawalRepository dbSyncWithdrawalRepository;

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
        poolHistory.setEpoch(epoch);
        poolHistory.setRewardAddress(dbSyncPoolUpdate.getStakeAddress().getView());

        BigInteger totalPoolRewards = dbSyncRewardRepository.getTotalPoolRewardsInEpoch(poolId, epoch);

        if (totalPoolRewards == null) {
            totalPoolRewards = BigInteger.ZERO;
        }

        DbSyncAdaPots dbSyncAdaPots = dbSyncAdaPotsRepository.findByEpoch(epoch + 1);
        BigInteger reserves = dbSyncAdaPots.getReserves();
        BigInteger adaInCirculation = TOTAL_LOVELACE.subtract(reserves);
        BigDecimal relativePoolStake = divide(activeStake, adaInCirculation);

        List<DbSyncPoolOwner> owners = dbSyncPoolOwnerRepository.getByPoolUpdateId(dbSyncPoolUpdate.getId());

        BigInteger totalActiveStakeOfOwners = BigInteger.ZERO;
        BigInteger totalMemberRewardsOfOwners = BigInteger.ZERO;

        for (DbSyncPoolOwner owner : owners) {
            Delegator delegator = poolHistory.getDelegator(owner.getStakeAddress().getView());
            if (delegator != null) {
                totalActiveStakeOfOwners = totalActiveStakeOfOwners.add(delegator.getActiveStake());
                totalMemberRewardsOfOwners = totalMemberRewardsOfOwners.add(PoolRewardCalculation.calculateMemberReward(totalPoolRewards,
                        poolHistory.getMargin(), poolHistory.getFixedCost(),
                        divide(delegator.getActiveStake(), adaInCirculation), relativePoolStake));
            }
        }

        BigInteger poolOperatorReward = PoolRewardCalculation.calculateLeaderReward(totalPoolRewards,
                poolHistory.getMargin(), poolHistory.getFixedCost(),
                divide(totalActiveStakeOfOwners, adaInCirculation), relativePoolStake);

        poolHistory.setPoolFees(poolOperatorReward.subtract(totalMemberRewardsOfOwners));
        poolHistory.setDelegatorRewards(totalPoolRewards.subtract(poolOperatorReward).add(totalMemberRewardsOfOwners));

        return poolHistory;
    }

    @Override
    public Double getPoolPledgeInEpoch(String poolId, int epoch) {
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
    public List<Reward> getRewardListForPoolInEpoch(int epoch, String poolId) {
        List <DbSyncReward> poolRewards = dbSyncRewardRepository.getMemberRewardListForPoolInEpoch(poolId, epoch);

        if (poolRewards.isEmpty()) {
            return List.of();
        }

        return poolRewards.stream().map(RewardMapper::fromDbSyncReward).toList();
    }

    @Override
    public BigInteger getTotalPoolRewardsInEpoch(String poolId, int epoch) {
        return dbSyncRewardRepository.getTotalPoolRewardsInEpoch(poolId, epoch);
    }

    public List<String> getPoolsThatProducedBlocksInEpoch(int epoch) {
        return dbSyncBlockRepository.getPoolsThatProducedBlocksInEpoch(epoch);
    }
}
