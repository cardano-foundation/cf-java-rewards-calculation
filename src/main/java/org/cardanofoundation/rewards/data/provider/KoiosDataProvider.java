package org.cardanofoundation.rewards.data.provider;

import lombok.RequiredArgsConstructor;
import org.cardanofoundation.rewards.config.KoiosClient;
import org.springframework.stereotype.Service;
import rest.koios.client.backend.api.account.model.AccountUpdate;
import rest.koios.client.backend.api.account.model.AccountUpdates;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.network.model.Totals;
import rest.koios.client.backend.api.pool.model.Pool;
import rest.koios.client.backend.api.pool.model.PoolUpdate;
import rest.koios.client.backend.factory.options.Options;
import rest.koios.client.backend.factory.options.filters.Filter;
import rest.koios.client.backend.factory.options.filters.FilterType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KoiosDataProvider {
    final KoiosClient koiosClient;

    public Totals getAdaPotsForEpoch(int epoch) throws ApiException {
        return koiosClient.networkService()
                .getHistoricalTokenomicStatsByEpoch(epoch)
                .getValue();
    }

    public String getTotalFeesForEpoch(int epoch) throws ApiException {
        return koiosClient.epochService().getEpochInformationByEpoch(epoch).getValue().getFees();
    }

    public int getTotalBlocksInEpoch(int epoch) throws ApiException {
        return koiosClient.epochService().getEpochInformationByEpoch(epoch).getValue().getBlkCount();
    }

    public int countRetiredPoolsWithDeregisteredRewardAddress(int epoch) throws ApiException {
        List<PoolUpdate> poolUpdates = koiosClient.poolService()
                .getPoolUpdates(Options.builder()
                        .option(Filter.of("active_epoch_no", FilterType.LTE, String.valueOf(epoch)))
                        .option(Filter.of("retiring_epoch", FilterType.EQ, String.valueOf(epoch)))
                        .build()).getValue();
        if (poolUpdates == null || poolUpdates.isEmpty()) {
            return 0;
        }

        List<String> rewardAddresses = poolUpdates.stream()
                .map(PoolUpdate::getRewardAddr)
                .toList();

        List<AccountUpdates> accountUpdates = koiosClient.accountService()
                .getAccountUpdates(rewardAddresses, null).getValue();

        int poolCount = 0;
        for (AccountUpdates accountUpdate : accountUpdates) {
            List<AccountUpdate> updates = accountUpdate.getUpdates();
            for (AccountUpdate update : updates) {
                if (update.getActionType().equals("deregistration") && update.getEpochNo() == epoch) {
                    poolCount = poolCount + 1;
                    break;
                }
            }
        }
        return poolCount;
    }
}
