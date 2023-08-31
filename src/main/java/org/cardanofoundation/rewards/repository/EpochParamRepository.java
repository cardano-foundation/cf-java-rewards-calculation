package org.cardanofoundation.rewards.repository;

import java.util.Optional;

import org.cardanofoundation.rewards.common.entity.EpochParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface EpochParamRepository extends JpaRepository<EpochParam, Long> {
  Optional<EpochParam> getEpochParamByEpochNo(@Param("epochNo") int epochNo);
}
