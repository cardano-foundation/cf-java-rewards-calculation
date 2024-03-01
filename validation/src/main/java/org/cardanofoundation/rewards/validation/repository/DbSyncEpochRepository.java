package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncEpoch;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db-sync")
public interface DbSyncEpochRepository extends ReadOnlyRepository<DbSyncEpoch, Long> {

    DbSyncEpoch findByNumber(Integer epochNumber);
}
