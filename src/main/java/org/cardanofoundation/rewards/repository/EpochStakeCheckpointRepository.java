package org.cardanofoundation.rewards.repository;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.EpochStakeCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EpochStakeCheckpointRepository extends JpaRepository<EpochStakeCheckpoint, Long> {
  List<EpochStakeCheckpoint> findByStakeAddressIn(@Param("stakeAddress") List<String> stakeAddress);
}
