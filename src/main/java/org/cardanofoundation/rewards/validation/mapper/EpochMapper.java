package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.calculation.entity.Epoch;
import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncEpoch;
import rest.koios.client.backend.api.epoch.model.EpochInfo;

import java.math.BigInteger;
import java.time.ZoneOffset;

public class EpochMapper {

    public static Epoch fromKoiosEpochInfo(EpochInfo epochInfo) {
        if (epochInfo == null) return null;

        BigInteger activeStake = null;
        if (epochInfo.getActiveStake() != null) {
            activeStake = new BigInteger(epochInfo.getActiveStake());
        }

        return Epoch.builder()
                .number(epochInfo.getEpochNo())
                .output(new BigInteger(epochInfo.getOutSum()))
                .fees(new BigInteger(epochInfo.getFees()))
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
