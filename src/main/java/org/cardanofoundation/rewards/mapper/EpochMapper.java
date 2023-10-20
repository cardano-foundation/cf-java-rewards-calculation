package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.Epoch;
import org.cardanofoundation.rewards.entity.jpa.DbSyncEpoch;
import rest.koios.client.backend.api.epoch.model.EpochInfo;

import java.time.ZoneOffset;

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
                .unixTimeFirstBlock(epochInfo.getFirstBlockTime())
                .unixTimeLastBlock(epochInfo.getLastBlockTime())
                .build();
    }

    public static Epoch fromDbSyncEpoch(DbSyncEpoch dbSyncEpoch) {
        return Epoch.builder()
                .number(dbSyncEpoch.getNumber())
                .fees(dbSyncEpoch.getFees())
                .blockCount(dbSyncEpoch.getBlockCount())
                .output(dbSyncEpoch.getOutput())
                .unixTimeFirstBlock(dbSyncEpoch.getStartTime().toEpochSecond(ZoneOffset.UTC))
                .unixTimeLastBlock(dbSyncEpoch.getEndTime().toEpochSecond(ZoneOffset.UTC))
                .build();
    }
}
