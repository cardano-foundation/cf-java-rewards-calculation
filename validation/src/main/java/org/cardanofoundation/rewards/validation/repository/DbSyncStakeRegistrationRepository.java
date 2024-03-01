package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncAccountRegistration;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.StakeAccountUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncStakeRegistrationRepository extends ReadOnlyRepository<DbSyncAccountRegistration, Long>{
    @Query("SELECT registration FROM DbSyncAccountRegistration registration WHERE " +
            "registration.address.view IN :addresses AND registration.epoch <= :epoch " +
            "ORDER BY registration.transaction.id DESC")
    List<DbSyncAccountRegistration> getLatestAccountRegistrationsUntilEpochForAddresses(
            List<String> addresses, Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT
                DISTINCT sa.view AS stakeAddress
            FROM
                stake_registration sr
            JOIN
                tx ON tx.id = sr.tx_id
            JOIN
                block ON block.id = tx.block_id AND  (block.epoch_no < :epoch OR
                                                     (block.epoch_no = :epoch AND block.epoch_slot_no < :stabilityWindow))
            JOIN
                stake_address sa ON sa.id = sr.addr_id
            WHERE
                sr.epoch_no <= :epoch AND
                sa.view IN :stakeAddresses""")
    HashSet<String> getStakeAddressesWithRegistrationsUntilEpoch(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow);
}
