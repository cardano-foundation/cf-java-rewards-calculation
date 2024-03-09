package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.calculation.domain.Epoch;
import rest.koios.client.backend.api.epoch.model.EpochInfo;
import java.math.BigInteger;

public class EpochMapper {

    public static Epoch fromKoiosEpochInfo(EpochInfo epochInfo) {
        if (epochInfo == null) return null;

        BigInteger activeStake = null;
        if (epochInfo.getActiveStake() != null) {
            activeStake = new BigInteger(epochInfo.getActiveStake());
        }

        return Epoch.builder()
                .number(epochInfo.getEpochNo())
                .fees(new BigInteger(epochInfo.getFees()))
                .blockCount(epochInfo.getBlkCount())
                .activeStake(activeStake)
                .build();
    }
}
