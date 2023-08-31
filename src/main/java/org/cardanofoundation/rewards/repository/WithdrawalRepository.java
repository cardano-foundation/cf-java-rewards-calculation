package org.cardanofoundation.rewards.repository;

import java.math.BigInteger;
import java.util.List;

import org.cardanofoundation.rewards.common.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.cardanofoundation.rewards.projection.DelegatorWithdrawalProjection;

@Repository
public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {

  @Query(
      "SELECT SUM(withdrawal.amount) FROM Withdrawal withdrawal "
          + "JOIN Tx tx ON withdrawal.tx = tx "
          + "JOIN Block block ON block.id = tx.blockId AND block.epochNo <= :epoch")
  BigInteger getTotalWithdrawalToEndEpoch(@Param("epoch") int epoch);

  @Query(
      "SELECT new org.cardanofoundation.rewards.projection.DelegatorWithdrawalProjection(wdr.stakeAddressId,SUM(wdr.amount))"
          + "FROM Withdrawal wdr "
          + "WHERE wdr.tx.id <= :txId "
          + "GROUP BY wdr.stakeAddressId")
  List<DelegatorWithdrawalProjection> findTotalWithdrawalsTilTx(@Param("txId") Long txId);
}
