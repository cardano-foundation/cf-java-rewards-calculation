package org.cardanofoundation.rewards.repository;

import java.util.List;
import java.util.Optional;

import org.cardanofoundation.rewards.common.entity.AdaPots;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdaPotsRepository extends JpaRepository<AdaPots, Long> {
  Optional<AdaPots> getAdaPotsByEpochNo(@Param("epochNo") int epochNo);

  @Query("SELECT MAX(adaPots.epochNo) FROM AdaPots adaPots")
  Optional<Integer> getTheLatestEpochHasAdaPots();

  List<AdaPots> getAdaPotsByEpochNoAfterOrderByEpochNo(@Param("epochNo") int epochNo);
}
