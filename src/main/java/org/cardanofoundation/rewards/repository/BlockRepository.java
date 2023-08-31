package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.common.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

  @Query(
      "SELECT blockOuter FROM Block blockOuter "
          + "WHERE blockOuter.id = "
          + "(SELECT MIN(block.id) FROM Block block WHERE block.epochNo = :epochNo)")
  Block getFirstBlockByEpochNo(@Param("epochNo") int epochNo);
}
