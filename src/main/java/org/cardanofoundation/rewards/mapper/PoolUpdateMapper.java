package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.PoolUpdate;
import org.cardanofoundation.rewards.enums.PoolStatus;

public class PoolUpdateMapper {

    public static PoolUpdate fromKoiosPoolUpdate(rest.koios.client.backend.api.pool.model.PoolUpdate poolUpdate) {
        if (poolUpdate == null) return null;

        return PoolUpdate.builder()
            .poolId(poolUpdate.getPoolIdBech32())
            .poolStatus(PoolStatus.fromString(poolUpdate.getPoolStatus()))
            .activeEpoch(Math.toIntExact(poolUpdate.getActiveEpochNo()))
            .fixedCost(Double.parseDouble(String.valueOf(poolUpdate.getFixedCost())))
            .margin(poolUpdate.getMargin())
            .pledge(Double.parseDouble(String.valueOf(poolUpdate.getPledge())))
            .transactionHash(poolUpdate.getTxHash())
            .rewardAddress(poolUpdate.getRewardAddr())
            .owners(poolUpdate.getOwners())
            .retiringEpoch(poolUpdate.getRetiringEpoch())
            .build();
    }
}
