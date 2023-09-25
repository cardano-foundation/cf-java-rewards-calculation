package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.PoolHistory;

public class PoolHistoryMapper {

    public static PoolHistory fromKoiosPoolHistory(rest.koios.client.backend.api.pool.model.PoolHistory poolHistory) {
        if (poolHistory == null) return null;

        return PoolHistory.builder()
            .activeStake(Double.parseDouble(poolHistory.getActiveStake()))
            .delegatorRewards(Double.parseDouble(poolHistory.getDelegRewards()))
            .poolFees(Double.parseDouble(poolHistory.getPoolFees()))
            .margin(Double.parseDouble(String.valueOf(poolHistory.getMargin())))
            .fixedCost(Double.parseDouble(String.valueOf(poolHistory.getFixedCost())))
            .blockCount(poolHistory.getBlockCnt())
            .epoch(poolHistory.getEpochNo())
            .build();
    }
}
