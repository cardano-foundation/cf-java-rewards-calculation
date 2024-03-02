package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncWithdrawal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;

public interface DbSyncWithdrawalRepository extends ReadOnlyRepository<DbSyncWithdrawal, Long>{

    @Query("""
           SELECT SUM(withdrawal.amount) from DbSyncWithdrawal AS withdrawal
             	WHERE withdrawal.transaction.block.epochNo = :epoch
            """)
    BigInteger getSumOfWithdrawalsInEpoch(@Param("epoch") Integer epoch);
}
