package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncBlock;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db-sync")
public interface DbSyncBlockRepository extends ReadOnlyRepository<DbSyncBlock, Long> {

    @Query("""
            select count(*) from DbSyncBlock AS block
                where block.epochNo = :epoch
                and block.slotLeader.pool.bech32PoolId = :poolId""")
    Integer getBlocksMadeByPoolInEpoch(String poolId, Integer epoch);

}
