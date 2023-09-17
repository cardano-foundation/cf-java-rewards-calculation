package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.ProtocolParameters;
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
}
