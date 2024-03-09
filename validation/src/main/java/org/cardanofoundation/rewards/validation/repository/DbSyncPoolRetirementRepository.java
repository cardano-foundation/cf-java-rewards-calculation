package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncPoolRetirement;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncPoolRetirementRepository extends ReadOnlyRepository<DbSyncPoolRetirement, Long>{

    @Query("""
            SELECT retirement FROM DbSyncPoolRetirement AS retirement
                WHERE retirement.retiringEpoch = :epoch AND retirement.announcedTransaction.block.epochNo <= :epoch""")
    List<DbSyncPoolRetirement> getPoolRetirementsByEpoch(@Param("epoch") Integer epoch);

    @Query("""
            SELECT retirement FROM DbSyncPoolRetirement AS retirement
                   WHERE retirement.announcedTransaction.block.epochNo <= :epoch
                   AND retirement.pool.bech32PoolId = :poolId
            ORDER BY retirement.announcedTransaction.id DESC LIMIT 1
            """)
    DbSyncPoolRetirement latestPoolRetirementUntilEpoch(@Param("poolId") String poolId,
                                                        @Param("epoch") Integer epoch);

}
