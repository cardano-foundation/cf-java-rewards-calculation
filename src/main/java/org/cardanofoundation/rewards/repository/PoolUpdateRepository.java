package org.cardanofoundation.rewards.repository;

import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.PoolUpdate;
import org.cardanofoundation.rewards.projection.PoolConfigProjection;
import org.cardanofoundation.rewards.projection.PoolUpdateProjection;
import org.cardanofoundation.rewards.projection.PoolUpdateRewardProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolUpdateRepository extends JpaRepository<PoolUpdate, Long> {

  @Query(
      "SELECT  poolUpdate.poolHashId as poolHashId, "
          + "poolUpdate.rewardAddrId as rewardAddrId "
          + "FROM PoolUpdate poolUpdate "
          + "WHERE poolUpdate.poolHashId IN :poolIds "
          + "AND poolUpdate.registeredTxId = "
          + "(SELECT MAX(poolUpdate2.registeredTxId) "
          + "FROM PoolUpdate poolUpdate2 "
          + "WHERE poolUpdate2.poolHashId = poolUpdate.poolHashId "
          + "AND poolUpdate2.registeredTxId <= :txId)")
  List<PoolUpdateRewardProjection> findLastPoolsRegistrationActivatedInEpoch(
      @Param("poolIds") Set<Long> poolIds, @Param("txId") long txId);

  @Query(
      "SELECT poolUpdate.poolHashId as poolHashId, "
          + "poolUpdate.registeredTxId as txId "
          + "FROM PoolUpdate poolUpdate "
          + "WHERE poolUpdate.registeredTxId = "
          + "(SELECT MAX(poolUpdate2.registeredTxId) "
          + "FROM PoolUpdate poolUpdate2 "
          + "WHERE poolUpdate2.poolHashId = poolUpdate.poolHashId "
          + "AND poolUpdate2.registeredTxId <= :txId)")
  List<PoolUpdateProjection> findLastPoolsRegistrationIdByTxId(@Param("txId") long txId);

  @Query(
      "SELECT poolUpdate.poolHashId as poolId, "
          + "poolUpdate.pledge as pledge, "
          + "poolUpdate.fixedCost as fixedCost, "
          + "poolUpdate.margin as margin, "
          + "poolUpdate.rewardAddrId as rewardAddressId, "
          + "poolUpdate.certIndex as certIndex, "
          + "poolUpdate.registeredTxId as txId, "
          + "poolUpdate.id as id "
          + "FROM PoolUpdate poolUpdate "
          + "WHERE poolUpdate.poolHashId NOT IN :poolIds "
          + "AND poolUpdate.activeEpochNo <= :epochNo")
  List<PoolConfigProjection> findAllActivePoolConfig(
      @Param("epochNo") int epochNo, @Param("poolIds") Set<Long> poolIds);

  @Query(
      "SELECT poolUpdate.poolHashId as poolId, "
          + "poolUpdate.pledge as pledge, "
          + "poolUpdate.fixedCost as fixedCost, "
          + "poolUpdate.margin as margin, "
          + "poolUpdate.rewardAddrId as rewardAddressId, "
          + "poolUpdate.certIndex as certIndex, "
          + "poolUpdate.registeredTxId as txId, "
          + "poolUpdate.id as poolUpdateId, "
          + "poolUpdate.activeEpochNo as activateEpochNo "
          + "FROM PoolUpdate poolUpdate "
          + "JOIN SlotLeader slotLeader "
          + "ON poolUpdate.poolHashId = slotLeader.poolHashId "
          + "JOIN Block b ON b.slotLeader = slotLeader AND b.epochNo = :epochNo "
          + "WHERE poolUpdate.activeEpochNo <= :epochNo")
  List<PoolConfigProjection> findAllPoolConfigHasMintedBlockInEpoch(@Param("epochNo") int epochNo);
}
