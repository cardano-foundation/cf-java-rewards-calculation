package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncEpochStake;
import org.cardanofoundation.rewards.entity.jpa.projection.PoolEpochStake;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DbSyncEpochStakeRepository extends ReadOnlyRepository<DbSyncEpochStake, Long> {

    @Query("""
        SELECT es.amount AS amount, es.epoch AS epoch, es.pool.bech32PoolId as poolId, es.stakeAddress.view as stakeAddress FROM DbSyncEpochStake AS es
        WHERE es.epoch=:epoch AND es.pool.bech32PoolId=:poolId""")
    List<PoolEpochStake> getPoolActiveStakeInEpoch(String poolId, Integer epoch);

    @Query("SELECT SUM(amount) FROM DbSyncEpochStake WHERE epoch=:epoch")
    Long getEpochStakeByEpoch(Integer epoch);
}
