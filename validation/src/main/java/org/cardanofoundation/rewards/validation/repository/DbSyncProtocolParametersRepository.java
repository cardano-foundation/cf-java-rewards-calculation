package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncProtocolParameters;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db-sync")
public interface DbSyncProtocolParametersRepository extends ReadOnlyRepository<DbSyncProtocolParameters, Long> {
    DbSyncProtocolParameters findByEpoch(Integer epoch);
}
