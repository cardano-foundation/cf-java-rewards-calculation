package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncPoolUpdate;
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
    DbSyncPoolUpdate findLastestUpdateForEpoch(String poolId, Integer epoch);

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

}
