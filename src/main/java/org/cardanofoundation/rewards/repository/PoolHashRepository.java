package org.cardanofoundation.rewards.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.PoolHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolHashRepository extends JpaRepository<PoolHash, Long> {

  @Query("SELECT ph.id FROM PoolHash ph WHERE ph.view IN :hashes")
  Set<Long> findIdByHashes(@Param("hashes") Set<String> hashes);

  List<PoolHash> findByViewIn(@Param("views") Collection<String> views);
}
