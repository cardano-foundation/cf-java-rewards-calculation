package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.PoolDeregistration;
import rest.koios.client.backend.api.pool.model.PoolUpdate;

public class PoolDeregistrationMapper {

    public static PoolDeregistration fromKoiosPoolUpdate(PoolUpdate poolUpdate) {
        return PoolDeregistration.builder()
                .epoch(poolUpdate.getRetiringEpoch())
                .poolId(poolUpdate.getPoolIdBech32())
                .rewardAddress(poolUpdate.getRewardAddr())
                .build();
    }
}
