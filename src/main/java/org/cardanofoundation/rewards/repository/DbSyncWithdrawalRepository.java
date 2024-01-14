package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncWithdrawal;
import org.springframework.data.jpa.repository.Query;

public interface DbSyncWithdrawalRepository extends ReadOnlyRepository<DbSyncWithdrawal, Long>{

    @Query("""
           SELECT SUM(withdrawal.amount) from DbSyncWithdrawal AS withdrawal
             	WHERE withdrawal.transaction.block.epochNo = :epoch
            """)
    Double getSumOfWithdrawalsInEpoch(Integer epoch);
}
