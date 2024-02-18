package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.PoolUpdate;
import org.cardanofoundation.rewards.enums.PoolStatus;

import java.math.BigInteger;

public class PoolUpdateMapper {

    public static PoolUpdate fromKoiosPoolUpdate(rest.koios.client.backend.api.pool.model.PoolUpdate poolUpdate) {
        if (poolUpdate == null) return null;

        return PoolUpdate.builder()
            .poolId(poolUpdate.getPoolIdBech32())
            .activeEpoch(Math.toIntExact(poolUpdate.getActiveEpochNo()))
            .fixedCost(new BigInteger(String.valueOf(poolUpdate.getFixedCost())))
            .margin(poolUpdate.getMargin())
            .pledge(new BigInteger(String.valueOf(poolUpdate.getPledge())))
            .transactionHash(poolUpdate.getTxHash())
            .rewardAddress(poolUpdate.getRewardAddr())
            .owners(poolUpdate.getOwners())
            .retiringEpoch(poolUpdate.getRetiringEpoch())
            .build();
    }

    public static PoolUpdate fromDbSyncPoolUpdate(org.cardanofoundation.rewards.entity.jpa.DbSyncPoolUpdate poolUpdate) {
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
