package org.cardanofoundation.rewards.repository;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.StakeDeregistration;
import org.cardanofoundation.rewards.projection.StakeIdTxIdProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StakeDeregistrationRepository extends JpaRepository<StakeDeregistration, Long> {

  @Query(
      "SELECT stakeDeregistration.stakeAddressId as stakeAddressId, "
          + "MAX(stakeDeregistration.txId) as txId "
          + "FROM StakeDeregistration stakeDeregistration "
          + "WHERE stakeDeregistration.stakeAddressId IN :stakeAddrIds "
          + "AND stakeDeregistration.txId < :txId "
          + "AND stakeDeregistration.epochNo <= :epochNo "
          + "GROUP BY stakeDeregistration.stakeAddressId")
  List<StakeIdTxIdProjection> getLastDeregistrationCertificateStakeIdAndTxIdByEpoch(
      @Param("epochNo") int epochNo,
      @Param("stakeAddrIds") List<Long> stakeAddrIds,
      @Param("txId") long txId);
}
