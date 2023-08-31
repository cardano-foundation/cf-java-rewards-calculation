package org.cardanofoundation.rewards.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.StakeAddress;
import org.cardanofoundation.rewards.projection.StakeAddressAndIdProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StakeAddressRepository extends JpaRepository<StakeAddress, Long> {

  List<StakeAddress> findByViewIn(@Param("views") Collection<String> views);

  @Query(
      "SELECT new org.cardanofoundation.rewards.projection.StakeAddressAndIdProjection(stakeAddress.view,stakeAddress.id) "
          + "FROM StakeAddress stakeAddress WHERE stakeAddress.view in :views")
  List<StakeAddressAndIdProjection> findByAddressesViewIn(@Param("views") Set<String> views);
}
