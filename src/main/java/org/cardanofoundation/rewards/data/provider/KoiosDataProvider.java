package org.cardanofoundation.rewards.data.provider;

import lombok.RequiredArgsConstructor;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.PoolHistory;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.cardanofoundation.rewards.mapper.AdaPotsMapper;
import org.cardanofoundation.rewards.mapper.EpochMapper;
import org.cardanofoundation.rewards.mapper.PoolHistoryMapper;
import org.cardanofoundation.rewards.mapper.ProtocolParametersMapper;
import org.springframework.stereotype.Service;
import rest.koios.client.backend.api.account.model.AccountUpdate;
import rest.koios.client.backend.api.account.model.AccountUpdates;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.block.model.Block;
import rest.koios.client.backend.api.epoch.model.EpochInfo;
import rest.koios.client.backend.api.epoch.model.EpochParams;
import rest.koios.client.backend.api.network.model.Totals;
import rest.koios.client.backend.api.pool.model.PoolUpdate;
import rest.koios.client.backend.factory.BackendFactory;
import rest.koios.client.backend.factory.BackendService;
import rest.koios.client.backend.factory.options.*;
import rest.koios.client.backend.factory.options.filters.Filter;
import rest.koios.client.backend.factory.options.filters.FilterType;
import java.util.ArrayList;
import java.util.List;

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
            if (epoch < 211) {
                epochEntity.setOBFTBlockCount(epochEntity.getBlockCount());
                epochEntity.setNonOBFTBlockCount(0);
            } else if (epoch > 256) {
                epochEntity.setOBFTBlockCount(0);
                epochEntity.setNonOBFTBlockCount(epochEntity.getBlockCount());
            } else {
                List<Block> blocks = new ArrayList<>();
                for (int offset = 0; offset < epochEntity.getBlockCount(); offset += 1000) {
                    blocks.addAll(koiosBackendService.getBlockService().getBlockList(Options.builder()
                            .option(Filter.of("epoch_no", FilterType.EQ, String.valueOf(epoch)))
                            .option(Offset.of(offset))
                            .build()).getValue());
                }

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

        return ProtocolParametersMapper.fromKoiosEpochParams(epochParams);
    }

    /*
       The deposit for pool registration is 500 ADA. The deposit is returned 2 epochs later when the pool is retired.
       If the stake address associated with the pool has been deregistered, the deposit is returned to the treasury.
     */
    public int countRetiredPoolsWithDeregisteredRewardAddress(int epoch) throws ApiException {
        List<PoolUpdate> poolUpdates = koiosBackendService.getPoolService()
                .getPoolUpdates(Options.builder()
                        .option(Filter.of("retiring_epoch", FilterType.LTE, String.valueOf(epoch)))
                        .option(Filter.of("retiring_epoch", FilterType.GTE, String.valueOf(epoch - 2)))
                        .build()).getValue();

        if (poolUpdates.isEmpty()) {
            return 0;
        }

        List<String> rewardAddresses = poolUpdates.stream()
                .map(PoolUpdate::getRewardAddr)
                .toList();

        List<AccountUpdates> accountUpdates = koiosBackendService.getAccountService()
                .getAccountUpdates(rewardAddresses, Options.EMPTY).getValue();

        int poolCount = 0;
        for (AccountUpdates accountUpdate : accountUpdates) {
            List<AccountUpdate> updates = accountUpdate.getUpdates();
            for (AccountUpdate update : updates) {
                if (update.getActionType().equals("deregistration") && update.getEpochNo() <= epoch) {
                    System.out.println("Epoch: " + epoch);
                    System.out.println(accountUpdate.getStakeAddress());
                    poolCount = poolCount + 1;
                    break;
                }
            }
        }
        return poolCount;
    }

    public PoolHistory getPoolHistory(String poolId, int epoch) {
        rest.koios.client.backend.api.pool.model.PoolHistory poolHistory = null;

        try {
            poolHistory = koiosBackendService.getPoolService()
                    .getPoolHistoryByEpoch(poolId, epoch, Options.EMPTY).getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return PoolHistoryMapper.fromKoiosPoolHistory(poolHistory);
    }

    @Override
    public Double getPoolPledgeInEpoch(String poolId, int epoch) {
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
            return Double.valueOf(poolUpdates.get(0).getPledge());
        }
    }

    public List<String> getPoolOwners(String poolId, int epoch) {
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

        if(poolUpdates.isEmpty()) {
            return List.of();
        } else {
            return poolUpdates.get(0).getOwners();
        }
    }

    public Double getActiveStakesOfAddressesInEpoch(List<String> stakeAddresses, int epoch) {
        Double activeStake = null;

        try {
            activeStake = koiosBackendService.getAccountService()
                .getAccountHistory(stakeAddresses, epoch, Options.EMPTY)
                .getValue().stream().map(accountHistory ->
                       Double.parseDouble(accountHistory.getHistory().get(0).getActiveStake()))
                .reduce((double) 0, Double::sum);
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return activeStake;
    }
}
