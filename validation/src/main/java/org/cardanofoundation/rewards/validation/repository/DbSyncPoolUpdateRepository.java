package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncPoolUpdate;
import org.cardanofoundation.rewards.validation.entity.projection.LatestPoolUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncPoolUpdateRepository extends ReadOnlyRepository<DbSyncPoolUpdate, Long> {

    @Query("""
            SELECT update FROM DbSyncPoolUpdate AS update
                WHERE update.pool.bech32PoolId = :poolId
                AND update.activeEpochNumber <= :epoch
            ORDER BY update.registeredTransaction.id DESC LIMIT 1""")
    DbSyncPoolUpdate findLastestActiveUpdateInEpoch(@Param("poolId") String poolId,
                                                    @Param("epoch") Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT pool_update.id, pool_hash.view AS poolId, pledge, margin,
            	   fixed_cost AS fixedCost, stake_address.view AS rewardAddress
            FROM pool_update
            	JOIN stake_address ON stake_address.id=pool_update.reward_addr_id
            	JOIN pool_hash ON pool_hash.id=pool_update.hash_id AND pool_hash.view IN :poolIds
            	JOIN (SELECT MAX(registered_tx_id) AS registered_tx_id, hash_id
            			FROM pool_update WHERE active_epoch_no <= :epoch
            			GROUP BY hash_id
            	) AS latest_update ON latest_update.hash_id=pool_update.hash_id AND latest_update.registered_tx_id=pool_update.registered_tx_id
            WHERE pool_update.active_epoch_no <= :epoch
            """)
    HashSet<LatestPoolUpdate> findLatestActiveUpdatesInEpoch(@Param("epoch") Integer epoch,
                                                             @Param("poolIds") List<String> poolIds);

    @Query("""
           SELECT update FROM DbSyncPoolUpdate AS update
               WHERE update.pool.bech32PoolId = :poolId
               AND update.registeredTransaction.id > :transactionId
               AND update.registeredTransaction.block.epochNo <= :epoch
           ORDER BY update.registeredTransaction.id DESC""")
    List<DbSyncPoolUpdate> findByBech32PoolIdAfterTransactionIdInEpoch(@Param("poolId") String poolId,
                                                                       @Param("transactionId") long transactionId,
                                                                       @Param("epoch") int epoch);

    @Query("""
           SELECT update FROM DbSyncPoolUpdate AS update
               WHERE update.pool.bech32PoolId = :poolId
               AND update.registeredTransaction.block.epochNo <= :epoch
           ORDER BY update.registeredTransaction.id DESC LIMIT 1""")
    DbSyncPoolUpdate findLatestUpdateInEpoch(@Param("poolId") String poolId,
                                             @Param("epoch") int epoch);

    @Query(nativeQuery = true, value = """
            WITH active_pool AS (
             SELECT DISTINCT pool_hash.view AS pool_id, pool_hash.id AS pool_hash_id, block.epoch_no AS epoch_no, latest_pool_update.reward_addr_id AS reward_addr_id
             FROM block
              JOIN slot_leader ON slot_leader.id=block.slot_leader_id
              JOIN pool_hash ON pool_hash.id=slot_leader.pool_hash_id
              JOIN (
               SELECT hash_id, reward_addr_id
               FROM pool_update WHERE pool_update.registered_tx_id IN (
                SELECT MAX(registered_tx_id) FROM pool_update WHERE active_epoch_no <= :epoch GROUP BY hash_id
               )
              ) AS latest_pool_update ON latest_pool_update.hash_id=slot_leader.pool_hash_id
             WHERE block.epoch_no = :epoch AND slot_leader.pool_hash_id is not NULL
            )
            SELECT DISTINCT p1.pool_id
            FROM active_pool p1
            JOIN active_pool p2
                 ON p1.epoch_no = p2.epoch_no
             AND p1.reward_addr_id = p2.reward_addr_id
             AND p1.pool_id != p2.pool_id
            WHERE NOT EXISTS (
             SELECT 1 FROM reward WHERE type = 'leader' AND pool_id=p1.pool_hash_id AND earned_epoch=:epoch
            )""")
    HashSet<String> findSharedPoolRewardAddressWithoutReward(@Param("epoch") int epoch);
}
