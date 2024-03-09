package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.calculation.domain.Delegator;
import org.cardanofoundation.rewards.validation.entity.projection.PoolEpochStake;

public class DelegatorMapper {
    public static Delegator fromPoolEpochStake(PoolEpochStake poolEpochStake) {
        return Delegator.builder()
                .stakeAddress(poolEpochStake.getStakeAddress())
                .activeStake(poolEpochStake.getAmount())
                .build();
    }
}
