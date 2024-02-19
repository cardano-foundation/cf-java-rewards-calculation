package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.calculation.entity.ProtocolParameters;
import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncProtocolParameters;
import rest.koios.client.backend.api.epoch.model.EpochParams;

public class ProtocolParametersMapper {

    public static ProtocolParameters fromKoiosEpochParams(EpochParams epochParams) {
        if (epochParams == null) return null;

        return ProtocolParameters.builder()
            .decentralisation(epochParams.getDecentralisation().doubleValue())
            .monetaryExpandRate(epochParams.getMonetaryExpandRate().doubleValue())
            .treasuryGrowRate(epochParams.getTreasuryGrowthRate().doubleValue())
            .optimalPoolCount(epochParams.getOptimalPoolCount())
            .poolOwnerInfluence(epochParams.getInfluence().doubleValue())
            .build();
    }

    public static ProtocolParameters fromDbSyncProtocolParameters(DbSyncProtocolParameters dbSyncProtocolParameters) {
        if (dbSyncProtocolParameters == null) return null;

        return ProtocolParameters.builder()
            .decentralisation(dbSyncProtocolParameters.getDecentralisation())
            .monetaryExpandRate(dbSyncProtocolParameters.getMonetaryExpandRate())
            .treasuryGrowRate(dbSyncProtocolParameters.getTreasuryGrowRate())
            .optimalPoolCount(dbSyncProtocolParameters.getOptimalPoolCount())
            .poolOwnerInfluence(dbSyncProtocolParameters.getPoolOwnerInfluence())
            .build();
    }
}
