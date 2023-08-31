package org.cardanofoundation.rewards.repository;

import java.math.BigInteger;

import org.cardanofoundation.rewards.common.entity.Tx;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TxRepository extends JpaRepository<Tx, Long> {

  @Query("SELECT SUM(tx.deposit) FROM Tx tx WHERE tx.id > :fromTxId AND tx.id <= :toTxId")
  BigInteger getDepositFromTxIdToTxId(
      @Param("fromTxId") long fromTxId, @Param("toTxId") long toTxId);

  @Query(
      "SELECT SUM(tx.fee) FROM Tx tx "
          + "INNER JOIN Block block ON tx.blockId = block.id "
          + "WHERE block.id = :blockId")
  BigInteger getFeeOfBlock(@Param("blockId") long blockId);

  @Query(
      "SELECT MAX(tx.id) "
          + "FROM Tx tx WHERE tx.blockId = "
          + "(SELECT MAX(block.id) FROM Block block "
          + "WHERE (block.epochNo <= :epochNo) "
          + "AND block.txCount > 0)")
  Long getLastTxIdByEpochNo(@Param("epochNo") int epochNo);

  @Query("SELECT MAX(tx.id) FROM Tx tx WHERE tx.blockId = :blockId")
  Long getMaxTxIdByBlockId(@Param("blockId") long blockId);

  @Query(
      "SELECT SUM(tx.fee) FROM Tx tx "
          + "JOIN Block block ON tx.blockId = block.id "
          + "WHERE block.epochNo = :epoch")
  BigInteger getFeeOfEpoch(@Param("epoch") int epoch);
}
