package org.cardanofoundation.rewards.validation.data.provider;

import lombok.RequiredArgsConstructor;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.validation.mapper.*;
import org.springframework.stereotype.Service;
import rest.koios.client.backend.api.account.model.AccountHistory;
import rest.koios.client.backend.api.account.model.AccountHistoryInner;
import rest.koios.client.backend.api.account.model.AccountUpdates;
import rest.koios.client.backend.api.base.Result;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.block.model.Block;
import rest.koios.client.backend.api.epoch.model.EpochInfo;
import rest.koios.client.backend.api.epoch.model.EpochParams;
import rest.koios.client.backend.api.network.model.Totals;
import rest.koios.client.backend.api.pool.model.PoolDelegatorHistory;
import rest.koios.client.backend.api.pool.model.PoolUpdate;
import rest.koios.client.backend.factory.BackendFactory;
import rest.koios.client.backend.factory.BackendService;
import rest.koios.client.backend.factory.options.*;
import rest.koios.client.backend.factory.options.filters.Filter;
import rest.koios.client.backend.factory.options.filters.FilterType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class KoiosDataProvider implements DataProvider {
    private final BackendService koiosBackendService;

    public KoiosDataProvider() {
        this.koiosBackendService = BackendFactory.getKoiosMainnetService();
    }

    public AdaPots getAdaPotsForEpoch(int epoch) {
        Totals totals = null;

        try {
            totals = koiosBackendService.getNetworkService()
                    .getHistoricalTokenomicStatsByEpoch(epoch)
                    .getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return AdaPotsMapper.fromKoiosTotals(totals);
    }

    public Epoch getEpochInfo(int epoch) {
        Epoch epochEntity = null;

        try {
            EpochInfo epochInfo = koiosBackendService.getEpochService()
                    .getEpochInformationByEpoch(epoch).getValue();

            epochEntity = EpochMapper.fromKoiosEpochInfo(epochInfo);
            List<Block> blocks = new ArrayList<>();
            for (int offset = 0; offset < epochEntity.getBlockCount(); offset += 1000) {
                blocks.addAll(koiosBackendService.getBlockService().getBlockList(Options.builder()
                        .option(Filter.of("epoch_no", FilterType.EQ, String.valueOf(epoch)))
                        .option(Offset.of(offset))
                        .build()).getValue());
            }
            epochEntity.setPoolsMadeBlocks(blocks.stream().map(Block::getPool).filter(Objects::nonNull).distinct().toList());
            if (epoch < 211) {
                epochEntity.setOBFTBlockCount(epochEntity.getBlockCount());
                epochEntity.setNonOBFTBlockCount(0);
            } else if (epoch > 256) {
                epochEntity.setOBFTBlockCount(0);
                epochEntity.setNonOBFTBlockCount(epochEntity.getBlockCount());
            } else {
                epochEntity.setOBFTBlockCount((int) blocks.stream().filter(block -> block.getPool() == null).count());
                epochEntity.setNonOBFTBlockCount((int) blocks.stream().filter(block -> block.getPool() != null).count());
            }
        } catch (ApiException e) {
            e.printStackTrace();
            epochEntity = null;
        }

        return epochEntity;
    }

    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        EpochParams epochParams = null;

        try {
            epochParams = koiosBackendService.getEpochService()
                    .getEpochParametersByEpoch(epoch).getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        if (epochParams == null) return new ProtocolParameters();

        return ProtocolParametersMapper.fromKoiosEpochParams(epochParams);
    }

    @Override
    public List<PoolHistory> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch) {
        return null;
    }

    public PoolHistory getPoolHistory(String poolId, int epoch) {
        rest.koios.client.backend.api.pool.model.PoolHistory poolHistory = null;

        try {
            poolHistory = koiosBackendService.getPoolService()
                    .getPoolHistoryByEpoch(poolId, epoch, Options.EMPTY).getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        PoolHistory history = PoolHistoryMapper.fromKoiosPoolHistory(poolHistory);

        if (history == null) return null;

        HashSet<Delegator> poolMemberInEpoch = getPoolMemberInEpoch(poolId, epoch);
        history.setDelegators(poolMemberInEpoch);
        return history;
    }

    @Override
    public BigInteger getPoolPledgeInEpoch(String poolId, int epoch) {
        List<PoolUpdate> poolUpdates = new ArrayList<>();
        try {
            poolUpdates = koiosBackendService.getPoolService().getPoolUpdatesByPoolBech32(poolId, Options.builder()
                    .option(Filter.of("active_epoch_no", FilterType.LTE, String.valueOf(epoch)))
                    .option(Order.by("block_time", SortType.DESC))
                    .option(Limit.of(1))
                    .build()).getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        if (poolUpdates.isEmpty()) {
            return null;
        } else {
            return new BigInteger(poolUpdates.get(0).getPledge());
        }
    }

    @Override
    public PoolOwnerHistory getHistoryOfPoolOwnersInEpoch(String poolId, int epoch) {
        List<String> poolOwnerAddresses = new ArrayList<>();

        try {
            List<PoolUpdate> poolUpdates = koiosBackendService.getPoolService()
                    .getPoolUpdatesByPoolBech32(poolId, Options.builder()
                            .option(Filter.of("active_epoch_no", FilterType.LTE, String.valueOf(epoch)))
                            .option(Order.by("block_time", SortType.DESC))
                            .option(Limit.of(1))
                            .build()).getValue();

            if (poolUpdates == null || poolUpdates.isEmpty()) return null;
            poolOwnerAddresses = poolUpdates.get(0).getOwners();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        if (poolOwnerAddresses.isEmpty()) return null;
        PoolOwnerHistory poolOwnerHistory = null;

        try {
            List<AccountHistory> accountHistories = koiosBackendService.getAccountService()
                    .getAccountHistory(poolOwnerAddresses, epoch, Options.EMPTY)
                    .getValue();

            if (accountHistories == null || accountHistories.isEmpty()) return null;

            BigInteger totalActiveStake = accountHistories.stream()
                    .map(AccountHistory::getHistory)
                    .flatMap(List::stream)
                    .map(AccountHistoryInner::getActiveStake)
                    .map(BigInteger::new)
                    .reduce(BigInteger.ZERO, BigInteger::add);

            poolOwnerHistory = PoolOwnerHistory.builder()
                    .activeStake(totalActiveStake)
                    .epoch(epoch)
                    .stakeAddresses(poolOwnerAddresses)
                    .build();

        } catch (ApiException e) {
            e.printStackTrace();
        }

        return poolOwnerHistory;
    }

    @Override
    public List<PoolDeregistration> getRetiredPoolsInEpoch(int epoch) {
        List<PoolDeregistration> retiredPools = new ArrayList<>();

        // TODO: It seems as this is not a sufficient method to get the retired pools
        try {
            List<PoolUpdate> poolUpdateList = koiosBackendService.getPoolService().getPoolUpdates(Options.builder()
                .option(Filter.of("active_epoch_no", FilterType.EQ, String.valueOf(epoch)))
                .option(Filter.of("retiring_epoch", FilterType.GT, String.valueOf(epoch - 1)))
                .option(Filter.of("retiring_epoch", FilterType.LTE, String.valueOf(epoch)))
                .build()).getValue();

            if (poolUpdateList == null) return List.of();

            retiredPools.addAll(poolUpdateList.stream()
                    .map(PoolDeregistrationMapper::fromKoiosPoolUpdate).toList());

        } catch (ApiException e) {
            e.printStackTrace();
        }

        return retiredPools;
    }

    @Override
    public List<AccountUpdate> getAccountUpdatesUntilEpoch(List<String> stakeAddresses, int epoch) {
        List<AccountUpdate> accountUpdates = new ArrayList<>();

        try {
            Result<List<AccountUpdates>> updates = koiosBackendService.getAccountService().getAccountUpdates(stakeAddresses, Options.EMPTY);
            List<AccountUpdates> accountUpdatesList = updates.getValue();
            if (accountUpdatesList != null) {
                accountUpdates.addAll(AccountUpdateMapper.fromKoiosAccountUpdates(accountUpdatesList));
                accountUpdates = accountUpdates.stream().filter(
                        accountUpdate -> accountUpdate.getEpoch() <= epoch)
                        .toList();
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return accountUpdates;
    }

    @Override
    public List<MirCertificate> getMirCertificatesInEpoch(int epoch) {
        return null;
    }

    @Override
    public int getPoolRegistrationsInEpoch(int epoch) {
        return 0;
    }

    @Override
    public List<org.cardanofoundation.rewards.calculation.domain.PoolUpdate> getPoolUpdateAfterTransactionIdInEpoch(String poolId, long transactionId, int epoch) {
        return null;
    }

    @Override
    public PoolDeregistration latestPoolRetirementUntilEpoch(String poolId, int epoch) {
        return null;
    }

    @Override
    public BigInteger getTransactionDepositsInEpoch(int epoch) {
        return null;
    }

    @Override
    public BigInteger getSumOfFeesInEpoch(int epoch) {
        return null;
    }

    @Override
    public BigInteger getSumOfWithdrawalsInEpoch(int epoch) {
        return null;
    }

    @Override
    public HashSet<Reward> getMemberRewardsInEpoch(int epoch) {
        return null;
    }

    @Override
    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch) {
        return null;
    }

    @Override
    public HashSet<PoolReward> getTotalPoolRewardsInEpoch(int epoch) {
        return null;
    }

    @Override
    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch) {
        return null;
    }

    @Override
    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow) {
        return null;
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilLastEpoch(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        return null;
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilNow(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        return null;
    }

    private HashSet<Delegator> getPoolMemberInEpoch(String poolId, int epoch) {
        HashSet<Delegator> delegators = new HashSet<>();
        try {
            List<PoolDelegatorHistory> poolDelegatorsHistory = koiosBackendService
                    .getPoolService().getPoolDelegatorsHistory(poolId, epoch, Options.EMPTY).getValue();
            for (PoolDelegatorHistory poolDelegator : poolDelegatorsHistory) {
                delegators.add(Delegator.builder()
                        .activeStake(new BigInteger(poolDelegator.getAmount()))
                        .stakeAddress(poolDelegator.getStakeAddress())
                        .build());
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return delegators;
    }
}
