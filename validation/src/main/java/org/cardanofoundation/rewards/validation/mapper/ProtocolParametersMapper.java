package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.calculation.domain.ProtocolParameters;
import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncProtocolParameters;
import rest.koios.client.backend.api.epoch.model.EpochParams;

import java.math.BigDecimal;

public class ProtocolParametersMapper {

    public static ProtocolParameters fromKoiosEpochParams(EpochParams epochParams) {
        if (epochParams == null) return null;

        return ProtocolParameters.builder()
            .decentralisation(epochParams.getDecentralisation())
            .monetaryExpandRate(epochParams.getMonetaryExpandRate())
            .treasuryGrowRate(epochParams.getTreasuryGrowthRate())
            .optimalPoolCount(epochParams.getOptimalPoolCount())
            .poolOwnerInfluence(epochParams.getInfluence())
            .build();
    }

    public static ProtocolParameters fromDbSyncProtocolParameters(DbSyncProtocolParameters dbSyncProtocolParameters) {
        if (dbSyncProtocolParameters == null) return new ProtocolParameters();

        return ProtocolParameters.builder()
            .decentralisation(BigDecimal.valueOf(dbSyncProtocolParameters.getDecentralisation()))
            .monetaryExpandRate(BigDecimal.valueOf(dbSyncProtocolParameters.getMonetaryExpandRate()))
            .treasuryGrowRate(BigDecimal.valueOf(dbSyncProtocolParameters.getTreasuryGrowRate()))
            .optimalPoolCount(dbSyncProtocolParameters.getOptimalPoolCount())
            .poolOwnerInfluence(BigDecimal.valueOf(dbSyncProtocolParameters.getPoolOwnerInfluence()))
            .build();
    }
}
