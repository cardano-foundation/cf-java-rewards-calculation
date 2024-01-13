package org.cardanofoundation.rewards.repository;

import com.bloxbean.cardano.client.transaction.spec.cert.PoolRetirement;
import org.cardanofoundation.rewards.entity.jpa.DbSyncPoolRetirement;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncPoolRetirementRepository extends ReadOnlyRepository<DbSyncPoolRetirement, Long>{

    @Query("""
            SELECT retirement FROM DbSyncPoolRetirement AS retirement
                WHERE retirement.retiringEpoch = :epoch AND retirement.announcedTransaction.block.epochNo <= :epoch""")
    List<DbSyncPoolRetirement> getPoolRetirementsByEpoch(Integer epoch);

}
