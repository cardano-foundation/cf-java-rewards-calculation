package org.cardanofoundation.rewards.data.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import rest.koios.client.backend.api.account.model.AccountUpdate;
import rest.koios.client.backend.api.account.model.AccountUpdates;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.epoch.model.EpochInfo;
import rest.koios.client.backend.api.epoch.model.EpochParams;
import rest.koios.client.backend.api.network.model.Totals;
import rest.koios.client.backend.api.pool.model.PoolHistory;
import rest.koios.client.backend.api.pool.model.PoolUpdate;
import rest.koios.client.backend.factory.BackendFactory;
import rest.koios.client.backend.factory.BackendService;
import rest.koios.client.backend.factory.options.Limit;
import rest.koios.client.backend.factory.options.Options;
import rest.koios.client.backend.factory.options.Order;
import rest.koios.client.backend.factory.options.SortType;
import rest.koios.client.backend.factory.options.filters.Filter;
import rest.koios.client.backend.factory.options.filters.FilterType;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KoiosDataProvider {
    private final BackendService koiosBackendService;

    public KoiosDataProvider() {
        this.koiosBackendService = BackendFactory.getKoiosMainnetService();
    }


    public Totals getAdaPotsForEpoch(int epoch) throws ApiException {
        return koiosBackendService.getNetworkService()
                .getHistoricalTokenomicStatsByEpoch(epoch)
                .getValue();
    }

    public EpochInfo getEpochInfo(int epoch) throws ApiException {
        return koiosBackendService.getEpochService()
                .getEpochInformationByEpoch(epoch).getValue();
    }

    public String getTotalFeesForEpoch(int epoch) throws ApiException {
        return koiosBackendService.getEpochService()
                .getEpochInformationByEpoch(epoch).getValue().getFees();
    }

    public int getTotalBlocksInEpoch(int epoch) throws ApiException {
        return koiosBackendService.getEpochService()
                .getEpochInformationByEpoch(epoch).getValue().getBlkCount();
    }

    public EpochParams getProtocolParametersForEpoch(int epoch) throws ApiException {
        return koiosBackendService.getEpochService()
                .getEpochParametersByEpoch(epoch).getValue();
    }

    public double getRelativeStakeForPool(String poolId, int epoch) throws ApiException {
        PoolHistory poolHistory = koiosBackendService.getPoolService().getPoolHistoryByEpoch(poolId, epoch, Options.EMPTY).getValue();
        return poolHistory.getActiveStakePct();
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

    public BigDecimal getDistributedRewardsInEpoch(int epoch) throws ApiException {
        return new BigDecimal(koiosBackendService.getEpochService()
                .getEpochInformationByEpoch(epoch).getValue().getTotalRewards());
    }

    public PoolHistory getPoolHistory(String poolId, int epoch) throws ApiException {
        return koiosBackendService.getPoolService()
                .getPoolHistoryByEpoch(poolId, epoch, Options.EMPTY).getValue();
    }

    public PoolUpdate getLatestPoolUpdateBeforeOrInEpoch(String poolId, int epoch) throws ApiException {
        List<PoolUpdate> poolUpdates = koiosBackendService.getPoolService().getPoolUpdatesByPoolBech32(poolId, Options.builder()
                .option(Filter.of("active_epoch_no", FilterType.LTE, String.valueOf(epoch)))
                .option(Order.by("block_time", SortType.DESC))
                .option(Limit.of(1))
                .build()).getValue();

        if (poolUpdates.isEmpty()) {
            return null;
        } else {
            return poolUpdates.get(0);
        }
    }

    public List<String> getPoolOwners(String poolId, int epoch) throws ApiException {
        List<PoolUpdate> poolUpdates = koiosBackendService.getPoolService().getPoolUpdatesByPoolBech32(poolId, Options.builder()
                .option(Filter.of("active_epoch_no", FilterType.LTE, String.valueOf(epoch)))
                .option(Order.by("block_time", SortType.DESC))
                .option(Limit.of(1))
                .build()).getValue();

        if(poolUpdates.isEmpty()) {
            return List.of();
        } else {
            return poolUpdates.get(0).getOwners();
        }
    }

    public BigDecimal getActiveStakesOfAddressesInEpoch(List<String> stakeAddresses, int epoch) throws ApiException {
        return koiosBackendService.getAccountService()
                .getAccountHistory(stakeAddresses, epoch, Options.EMPTY)
                .getValue().stream().map(accountHistory ->
                        new BigDecimal(accountHistory.getHistory().get(0).getActiveStake()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
