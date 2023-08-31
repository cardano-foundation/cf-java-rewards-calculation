package org.cardanofoundation.rewards.repository;

import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.Delegation;
import org.cardanofoundation.rewards.projection.DelegatorStakeUTXOProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DelegationRepository extends JpaRepository<Delegation, Long> {
  @Query(
      "SELECT new org.cardanofoundation.rewards.projection.DelegatorStakeUTXOProjection(d.stakeAddressId,SUM(addressTxBalance.balance),  d.poolHash.id) "
          + "FROM Delegation d "
          + "LEFT JOIN StakeDeregistration de ON de.addr.id = d.stakeAddressId AND de.txId ="
          + "(SELECT MAX(stakeDereg.txId) FROM StakeDeregistration stakeDereg WHERE "
          + "stakeDereg.addr.id = d.stakeAddressId AND stakeDereg.tx.id <= :txId) "
          + "LEFT JOIN StakeRegistration re ON re.addr.id = d.stakeAddressId AND re.txId = "
          + "(SELECT MAX(stakeReg.txId) FROM StakeRegistration stakeReg WHERE "
          + "stakeReg.addr.id = d.stakeAddressId AND stakeReg.tx.id <= :txId) "
          + "LEFT JOIN Address address ON address.stakeAddressId = d.stakeAddressId "
          + "LEFT JOIN AddressTxBalance addressTxBalance ON addressTxBalance.addressId = address.id and addressTxBalance.txId <= :txId "
          + "WHERE d.id IN "
          + "(SELECT MAX(deleIn.id) FROM Delegation deleIn WHERE deleIn.activeEpochNo <= :epochNo "
          + "GROUP BY (deleIn.stakeAddressId)) "
          + "AND (de.tx.id  < re.tx.id OR de IS NULL) "
          + "AND d.txId >= re.txId "
          + "AND (d.poolHash.id IN :poolIds)"
          + "GROUP BY d.stakeAddressId, d.poolHash.id, d.txId")
  List<DelegatorStakeUTXOProjection> findTotalStakedTilTx(
      @Param("txId") Long txId, @Param("epochNo") int epochNo, @Param("poolIds") Set<Long> poolIds);

  @Query(
      "SELECT delegation.stakeAddressId FROM Delegation delegation "
          + "LEFT JOIN StakeDeregistration de ON de.addr.id = delegation.stakeAddressId AND de.txId ="
          + "(SELECT MAX(stakeDereg.txId) FROM StakeDeregistration stakeDereg WHERE "
          + "stakeDereg.addr.id = delegation.stakeAddressId AND stakeDereg.tx.id <= :txId) "
          + "LEFT JOIN StakeRegistration re ON re.addr.id = delegation.stakeAddressId AND re.txId = "
          + "(SELECT MAX(stakeReg.txId) FROM StakeRegistration stakeReg WHERE "
          + "stakeReg.addr.id = delegation.stakeAddressId AND stakeReg.tx.id <= :txId) "
          + "WHERE delegation.id IN "
          + "(SELECT MAX(deleIn.id) FROM Delegation deleIn WHERE deleIn.activeEpochNo <= :epochNo "
          + "AND deleIn.stakeAddressId IN :stakeAddressId "
          + "GROUP BY (deleIn.stakeAddressId)) "
          + "AND (de.tx.id  < re.tx.id OR de IS NULL) ")
  Set<Long> getAllStakeAddressIdIsRegisterActivated(
      @Param("txId") Long txId,
      @Param("epochNo") int epochNo,
      @Param("stakeAddressId") Set<Long> stakeAddressId);
}
