package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncPoolUpdate;
import org.cardanofoundation.rewards.entity.jpa.projection.LatestPoolUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncPoolUpdateRepository extends ReadOnlyRepository<DbSyncPoolUpdate, Long> {

    @Query("""
            SELECT update FROM DbSyncPoolUpdate AS update
                WHERE update.pool.bech32PoolId = :poolId
                AND update.activeEpochNumber <= :epoch
            ORDER BY update.registeredTransaction.id DESC LIMIT 1""")
    DbSyncPoolUpdate findLastestActiveUpdateInEpoch(String poolId, Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT pool_update.id, pool_hash.view AS poolId, pledge, margin,
                   fixed_cost AS fixedCost, stake_address.view AS rewardAddress
            FROM pool_update
                JOIN stake_address ON stake_address.id=pool_update.reward_addr_id
                JOIN pool_hash ON pool_hash.id=hash_id
            WHERE pool_update.registered_tx_id IN (
                SELECT MAX(registered_tx_id) FROM pool_update WHERE active_epoch_no <= :epoch GROUP BY hash_id
            );""")
    List<LatestPoolUpdate> findLatestActiveUpdatesInEpoch(Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT COUNT(*) FROM (
            	SELECT hash_id, min(registered_tx_id) as registered_tx_id
            	FROM pool_update
            	GROUP BY hash_id) AS pool_registrations
            JOIN tx ON pool_registrations.registered_tx_id=tx.id JOIN block ON tx.block_id=block.id WHERE block.epoch_no=:epoch
            """)
    Integer countPoolRegistrationsInEpoch(Integer epoch);

    @Query("""
           SELECT update FROM DbSyncPoolUpdate AS update
               WHERE update.pool.bech32PoolId = :poolId
               AND update.registeredTransaction.id > :transactionId
               AND update.registeredTransaction.block.epochNo <= :epoch
           ORDER BY update.registeredTransaction.id DESC""")
    List<DbSyncPoolUpdate> findByBech32PoolIdAfterTransactionIdInEpoch(String poolId, long transactionId, int epoch);

    @Query("""
           SELECT update FROM DbSyncPoolUpdate AS update
               WHERE update.pool.bech32PoolId = :poolId
               AND update.registeredTransaction.block.epochNo <= :epoch
           ORDER BY update.registeredTransaction.id DESC LIMIT 1""")
    DbSyncPoolUpdate findLatestUpdateInEpoch(String poolId, int epoch);
}
