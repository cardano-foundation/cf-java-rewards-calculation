package org.cardanofoundation.rewards.data.provider;

import lombok.RequiredArgsConstructor;
import org.cardanofoundation.rewards.config.KoiosClient;
import org.springframework.stereotype.Service;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.network.model.Totals;

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
}
