package org.cardanofoundation.rewards.repository;

import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.PoolRetire;
import org.cardanofoundation.rewards.projection.PoolUpdateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolRetireRepository extends JpaRepository<PoolRetire, Long> {

  @Query(
      "SELECT poolRetire.poolHashId FROM PoolRetire poolRetire "
          + "JOIN PoolUpdate poolUpdate ON poolRetire.poolHashId = poolUpdate.poolHashId "
          + "AND poolRetire.announcedTxId <= :txId "
          + "AND poolRetire.announcedTxId >= poolUpdate.registeredTxId "
          + "AND poolRetire.retiringEpoch = :epochNo "
          + "AND poolRetire.announcedTxId = "
          + "(SELECT MAX(poolRetire2.announcedTxId) FROM PoolRetire poolRetire2 "
          + "WHERE poolRetire2.poolHashId = poolRetire.poolHashId "
          + "AND poolRetire2.announcedTxId <= :txId) "
          + "AND poolUpdate.registeredTxId = "
          + "(SELECT MAX(poolUpdate2.registeredTxId) FROM PoolUpdate poolUpdate2 "
          + "WHERE poolUpdate2.poolHashId = poolRetire.poolHashId "
          + "AND poolUpdate2.registeredTxId <= :txId) ")
  Set<Long> getPoolIdRetiredTilTxInEpoch(@Param("txId") long txId, @Param("epochNo") int epochNo);

  @Query(
      "SELECT poolRetire.poolHashId as poolHashId, "
          + "poolRetire.announcedTxId as txId "
          + "FROM PoolRetire poolRetire "
          + "WHERE poolRetire.announcedTxId = "
          + "(SELECT MAX(poolRetire2.announcedTxId) FROM PoolRetire poolRetire2 "
          + "WHERE poolRetire2.poolHashId = poolRetire.poolHashId "
          + "AND poolRetire2.announcedTxId <= :txId AND poolRetire.retiringEpoch <= :epochNo) ")
  List<PoolUpdateProjection> getLastPoolRetired(
      @Param("txId") long txId, @Param("epochNo") int epochNo);

  @Query(
      "SELECT poolRetire.poolHashId FROM PoolRetire poolRetire "
          + "JOIN PoolUpdate poolUpdate ON poolRetire.poolHashId = poolUpdate.poolHashId "
          + "AND poolRetire.announcedTxId <= :txId "
          + "AND poolRetire.announcedTxId > poolUpdate.registeredTxId "
          + "AND poolRetire.retiringEpoch < :epochNo "
          + "AND poolRetire.announcedTxId = "
          + "(SELECT MAX(poolRetire2.announcedTxId) FROM PoolRetire poolRetire2 "
          + "WHERE poolRetire2.poolHashId = poolRetire.poolHashId "
          + "AND poolRetire2.announcedTxId <= :txId) "
          + "AND poolUpdate.registeredTxId = "
          + "(SELECT MAX(poolUpdate2.registeredTxId) FROM PoolUpdate poolUpdate2 "
          + "WHERE poolUpdate2.poolHashId = poolRetire.poolHashId "
          + "AND poolUpdate2.registeredTxId <= :txId) ")
  Set<Long> getPoolIdRetiredTilEpoch(@Param("txId") long txId, @Param("epochNo") int epochNo);
}
