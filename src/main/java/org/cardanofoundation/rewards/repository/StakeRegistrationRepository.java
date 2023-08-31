package org.cardanofoundation.rewards.repository;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.StakeRegistration;
import org.cardanofoundation.rewards.projection.StakeIdTxIdProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StakeRegistrationRepository extends JpaRepository<StakeRegistration, Long> {

  @Query(
      "SELECT stakeRegistration.stakeAddressId as stakeAddressId, "
          + "MAX(stakeRegistration.txId) as txId "
          + "FROM StakeRegistration stakeRegistration "
          + "WHERE stakeRegistration.stakeAddressId IN :stakeAddrIds "
          + "AND stakeRegistration.txId < :txId "
          + "AND stakeRegistration.epochNo <= :epochNo "
          + "GROUP BY stakeRegistration.stakeAddressId")
  List<StakeIdTxIdProjection> getLastCertificateStakeIdAndTxIdByEpoch(
      @Param("epochNo") int epochNo,
      @Param("stakeAddrIds") List<Long> stakeAddrIds,
      @Param("txId") long txId);
}
