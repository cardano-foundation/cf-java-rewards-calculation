package org.cardanofoundation.rewards.repository;

import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EpochStakeRepository extends JpaRepository<EpochStake, Long> {
  @Query("SELECT epochStake FROM EpochStake epochStake WHERE epochStake.epochNo = :epochNo")
  List<EpochStake> findEpochStakeByEpochNo(@Param("epochNo") int epochNo);

  @Query(
      "SELECT DISTINCT(epochStake.poolId) FROM EpochStake epochStake WHERE epochStake.epochNo = :epochNo")
  Set<Long> getPoolIdInEpoch(@Param("epochNo") int epochNo);
}
