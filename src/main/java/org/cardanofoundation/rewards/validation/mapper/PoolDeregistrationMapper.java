package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncPoolRetirement;
import org.cardanofoundation.rewards.calculation.entity.PoolDeregistration;
import rest.koios.client.backend.api.pool.model.PoolUpdate;

public class PoolDeregistrationMapper {

    public static PoolDeregistration fromKoiosPoolUpdate(PoolUpdate poolUpdate) {
        return PoolDeregistration.builder()
                .retiringEpoch(poolUpdate.getRetiringEpoch())
                .poolId(poolUpdate.getPoolIdBech32())
                .rewardAddress(poolUpdate.getRewardAddr())
                .build();
    }

    public static PoolDeregistration fromDbSyncPoolRetirement(DbSyncPoolRetirement dbSyncPoolRetirement) {
        return PoolDeregistration.builder()
                .retiringEpoch(dbSyncPoolRetirement.getRetiringEpoch())
                .poolId(dbSyncPoolRetirement.getPool().getBech32PoolId())
                .announcedTransactionId(dbSyncPoolRetirement.getAnnouncedTransaction().getId())
                .build();
    }
}
