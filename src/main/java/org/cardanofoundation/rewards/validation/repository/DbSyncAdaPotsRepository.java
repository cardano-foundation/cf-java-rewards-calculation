package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncAdaPots;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db-sync")
public interface DbSyncAdaPotsRepository extends ReadOnlyRepository<DbSyncAdaPots, Long> {
    DbSyncAdaPots findByEpoch(Integer epoch);
}
