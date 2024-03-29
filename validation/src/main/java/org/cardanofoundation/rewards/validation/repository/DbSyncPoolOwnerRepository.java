package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncPoolOwner;
import org.cardanofoundation.rewards.validation.entity.projection.PoolOwner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncPoolOwnerRepository extends ReadOnlyRepository<DbSyncPoolOwner, Long> {

    @Query(nativeQuery = true, value= """
            SELECT stake_address.view AS stakeAddress, pool_hash.view AS poolId FROM pool_owner
            	JOIN pool_update ON pool_update.id=pool_owner.pool_update_id
            	JOIN pool_hash ON pool_hash.id=pool_update.hash_id
            	JOIN stake_address ON pool_owner.addr_id=stake_address.id
            WHERE pool_update_id IN :poolUpdateIds""")
    List<PoolOwner> getOwnersByPoolUpdateIds(@Param("poolUpdateIds") List<Long> poolUpdateIds);
}
