package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncBlock;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.PoolBlocks;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncBlockRepository extends ReadOnlyRepository<DbSyncBlock, Long> {

    @Query(nativeQuery = true, value = """
            SELECT count(*) from block
            LEFT JOIN slot_leader ON block.slot_leader_id=slot_leader.id
            LEFT JOIN pool_hash ON pool_hash.id=slot_leader.pool_hash_id
            WHERE block.epoch_no = :epoch and pool_hash.view = :poolId
            """)
    Integer getBlocksMadeByPoolInEpoch(@Param("poolId") String poolId,
                                       @Param("epoch") Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT count(*) as blockCount, pool_hash.view as poolId from block
            LEFT JOIN slot_leader ON block.slot_leader_id=slot_leader.id
            LEFT JOIN pool_hash ON pool_hash.id=slot_leader.pool_hash_id
            WHERE block.epoch_no = :epoch AND pool_hash.view IS NOT NULL GROUP BY pool_hash.view;
            """)
    List<PoolBlocks> getAllBlocksMadeByPoolsInEpoch(@Param("epoch") Integer epoch);

   @Query("""
              select count(*) from DbSyncBlock AS block
                    where block.epochNo = :epoch
                    and block.slotLeader.pool is NOT NULL
           """)
    Integer getNonOBFTBlocksInEpoch(@Param("epoch") Integer epoch);

    @Query("""
              select count(*) from DbSyncBlock AS block
                    where block.epochNo = :epoch
                    and block.slotLeader.pool is NULL
           """)
    Integer getOBFTBlocksInEpoch(@Param("epoch") Integer epoch);

    @Query("""
            select distinct block.slotLeader.pool.bech32PoolId from DbSyncBlock AS block
                where block.epochNo = :epoch
                and block.slotLeader.pool is not NULL
           """)
    List<String> getPoolsThatProducedBlocksInEpoch(@Param("epoch") Integer epoch);
}
