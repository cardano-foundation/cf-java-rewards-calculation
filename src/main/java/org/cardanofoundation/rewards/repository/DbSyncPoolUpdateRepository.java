package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncPoolUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db-sync")
public interface DbSyncPoolUpdateRepository extends ReadOnlyRepository<DbSyncPoolUpdate, Long> {

    @Query("""
            SELECT update FROM DbSyncPoolUpdate AS update
                WHERE update.pool.bech32PoolId = :poolId
                AND update.activeEpochNumber <= :epoch+1
            ORDER BY update.registeredTransaction.id DESC LIMIT 1""")
    DbSyncPoolUpdate findLastestUpdateForEpoch(String poolId, Integer epoch);

    @Query("""
            SELECT COUNT(update) FROM DbSyncPoolUpdate AS update
            WHERE update.registeredTransaction.block.epochNo = :epoch
            AND update.registeredTransaction.deposit = 500000000
            """)
    Integer countPoolRegistrationsInEpoch(Integer epoch);
}
