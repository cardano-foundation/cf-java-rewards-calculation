package org.cardanofoundation.rewards.repository;

import java.math.BigInteger;

import org.cardanofoundation.rewards.common.entity.AddressTxBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressTxBalanceRepository extends JpaRepository<AddressTxBalance, Long> {

  @Query(
      "SELECT SUM(addressTxBalance.balance) FROM AddressTxBalance addressTxBalance WHERE addressTxBalance.tx.id <= :txId")
  BigInteger getTotalSupplyOfLovelaceFromBeginToTxId(@Param("txId") long txId);
}
