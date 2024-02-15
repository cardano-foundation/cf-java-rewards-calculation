package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncTransaction;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
@Profile("db-sync")
public interface DbSyncTransactionRepository extends ReadOnlyRepository<DbSyncTransaction, Long> {
    @Query("SELECT SUM(deposit) FROM DbSyncTransaction WHERE block.epochNo = :epoch")
    BigInteger getSumOfDepositsInEpoch(Integer epoch);

    @Query("SELECT SUM(fee) FROM DbSyncTransaction WHERE block.epochNo = :epoch")
    BigInteger getSumOfFeesInEpoch(Integer epoch);
}
