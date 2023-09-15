package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.Epoch;
import rest.koios.client.backend.api.epoch.model.EpochInfo;

public class EpochMapper {

    public static Epoch fromKoiosEpochInfo(EpochInfo epochInfo) {
        if (epochInfo == null) return null;

        Double activeStake = null;
        if (epochInfo.getActiveStake() != null) {
            activeStake = Double.valueOf(epochInfo.getActiveStake());
        }

        return Epoch.builder()
                .number(epochInfo.getEpochNo())
                .output(Double.valueOf(epochInfo.getOutSum()))
                .fees(Double.valueOf(epochInfo.getFees()))
                .blockCount(epochInfo.getBlkCount())
                .activeStake(activeStake)
                .build();
    }
}
