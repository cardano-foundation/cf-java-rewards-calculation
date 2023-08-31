package org.cardanofoundation.rewards.repository;

import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.PoolOwner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PoolOwnerRepository extends JpaRepository<PoolOwner, Long> {

  @Query("SELECT po FROM PoolOwner po WHERE po.poolUpdateId IN :ids")
  List<PoolOwner> findAllPoolOwnerByPoolUpdateIds(@Param("ids") Set<Long> ids);
}
