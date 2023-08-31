package org.cardanofoundation.rewards.repository;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.SlotLeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.cardanofoundation.rewards.projection.PoolMintBlockProjection;

public interface SlotLeaderRepository extends JpaRepository<SlotLeader, Long> {

  @Query(
      "SELECT new org.cardanofoundation.rewards.projection.PoolMintBlockProjection(slotLeader.poolHashId, COUNT(slotLeader.id)) "
          + "FROM SlotLeader slotLeader "
          + "JOIN Block block ON slotLeader.id = block.slotLeaderId "
          + "WHERE block.epochNo = :epoch AND slotLeader.poolHashId IS NOT NULL "
          + "GROUP BY slotLeader.poolHashId")
  List<PoolMintBlockProjection> getPoolMintNumberBlockInEpoch(@Param("epoch") int epoch);

  @Query(
      "SELECT COUNT(*) FROM Block block "
          + "JOIN SlotLeader slotleader ON block.slotLeaderId = slotleader.id "
          + "WHERE block.epochNo = :epoch AND slotLeader.poolHashId IS NOT NULL ")
  Integer countBlockMintedByPoolInEpoch(@Param("epoch") int epoch);
}
