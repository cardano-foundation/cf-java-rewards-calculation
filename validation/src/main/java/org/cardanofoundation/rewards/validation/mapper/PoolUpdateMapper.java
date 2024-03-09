package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncPoolUpdate;
import org.cardanofoundation.rewards.calculation.domain.PoolUpdate;

public class PoolUpdateMapper {

    public static PoolUpdate fromDbSyncPoolUpdate(DbSyncPoolUpdate poolUpdate) {
        if (poolUpdate == null) return null;

        return PoolUpdate.builder()
                .poolId(poolUpdate.getPool().getBech32PoolId())
                .fixedCost(poolUpdate.getFixedCost())
                .margin(poolUpdate.getMargin())
                .pledge(poolUpdate.getPledge())
                .rewardAddress(poolUpdate.getStakeAddress().getView())
                .activeEpoch(poolUpdate.getActiveEpochNumber().intValue())
                .build();
    }
}
