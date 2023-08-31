package org.cardanofoundation.rewards.repository;

import java.util.List;
import java.util.Optional;

import org.cardanofoundation.rewards.common.entity.Epoch;
import org.cardanofoundation.rewards.common.enumeration.EraType;
import org.cardanofoundation.rewards.projection.EpochProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface EpochRepository extends JpaRepository<Epoch, Long> {
  @Transactional(readOnly = true)
  Optional<Epoch> findEpochByNo(Integer no);

  @Query("SELECT MAX(epoch.no) FROM Epoch epoch")
  Integer getCurrentEpoch();

  @Query("SELECT epoch FROM Epoch epoch WHERE epoch.era != :type ORDER BY epoch.no ASC")
  List<Epoch> getNextEpochByEraTypeGreater(@Param("type") EraType type, Pageable pageable);

  @Query(
      "SELECT block.epochNo as epochNo,"
          + "SUM(tx.fee) as fee "
          + "FROM Block block "
          + "JOIN Tx tx on tx.blockId = block.id "
          + "WHERE block.epochNo >= :epochNo "
          + "GROUP BY block.epochNo ORDER BY block.epochNo ASC")
  List<EpochProjection> getEpochFeeByEpochGreater(@Param("epochNo") int epochNo);
}
