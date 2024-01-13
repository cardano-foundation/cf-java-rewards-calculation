package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.PoolDeregistration;
import rest.koios.client.backend.api.pool.model.PoolUpdate;

public class PoolDeregistrationMapper {

    public static PoolDeregistration fromKoiosPoolUpdate(PoolUpdate poolUpdate) {
        return PoolDeregistration.builder()
                .retiringEpoch(poolUpdate.getRetiringEpoch())
                .poolId(poolUpdate.getPoolIdBech32())
                .rewardAddress(poolUpdate.getRewardAddr())
                .build();
    }

    public static PoolDeregistration fromDbSyncPoolRetirement(org.cardanofoundation.rewards.entity.jpa.DbSyncPoolRetirement dbSyncPoolRetirement) {
        return PoolDeregistration.builder()
                .retiringEpoch(dbSyncPoolRetirement.getRetiringEpoch())
                .poolId(dbSyncPoolRetirement.getPool().getBech32PoolId())
                .announcedTransactionId(dbSyncPoolRetirement.getAnnouncedTransaction().getId())
                .build();
    }
}
