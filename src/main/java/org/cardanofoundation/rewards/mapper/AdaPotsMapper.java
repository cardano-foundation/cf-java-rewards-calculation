package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.jpa.DbSyncAdaPots;
import rest.koios.client.backend.api.network.model.Totals;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;

public class AdaPotsMapper {

    public static AdaPots fromKoiosTotals (Totals totals) {
        if (totals == null) return null;

        return AdaPots.builder()
                .treasury(Double.parseDouble(totals.getTreasury()))
                .reserves(Double.parseDouble(totals.getReserves()))
                .rewards(Double.parseDouble(totals.getReward()))
                .adaInCirculation(Double.parseDouble(totals.getSupply()))
                .epoch(totals.getEpochNo())
                .build();
    }

    public static AdaPots fromDbSyncAdaPots (DbSyncAdaPots dbSyncAdaPots) {
        return AdaPots.builder()
                .treasury(dbSyncAdaPots.getTreasury())
                .reserves(dbSyncAdaPots.getReserves())
                .rewards(dbSyncAdaPots.getRewards())
                .deposits(dbSyncAdaPots.getDeposits())
                .fees(dbSyncAdaPots.getFees())
                .adaInCirculation(dbSyncAdaPots.getUtxo())
                .epoch(dbSyncAdaPots.getEpoch())
                .build();
    }
}
