package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.AdaPots;
import rest.koios.client.backend.api.network.model.Totals;

public class AdaPotsMapper {

    public static AdaPots fromKoiosTotals (Totals totals) {
        if (totals == null) return null;

        return AdaPots.builder()
                .treasury(Double.parseDouble(totals.getTreasury()))
                .reserves(Double.parseDouble(totals.getReserves()))
                .rewards(Double.parseDouble(totals.getReward()))
                .epoch(totals.getEpochNo())
                .build();
    }
}
