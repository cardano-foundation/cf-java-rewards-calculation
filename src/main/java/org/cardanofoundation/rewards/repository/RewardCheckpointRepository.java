package org.cardanofoundation.rewards.repository;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.RewardCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RewardCheckpointRepository extends JpaRepository<RewardCheckpoint, Long> {

  List<RewardCheckpoint> findByStakeAddressIn(@Param("stakeAddress") List<String> stakeAddress);
}
