package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncTransaction;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
@Profile("db-sync")
public interface DbSyncTransactionRepository extends ReadOnlyRepository<DbSyncTransaction, Long> {
    @Query("SELECT SUM(deposit) FROM DbSyncTransaction WHERE block.epochNo = :epoch")
    BigInteger getSumOfDepositsInEpoch(@Param("epoch") Integer epoch);

    @Query("SELECT SUM(fee) FROM DbSyncTransaction WHERE block.epochNo = :epoch")
    BigInteger getSumOfFeesInEpoch(@Param("epoch") Integer epoch);
}
