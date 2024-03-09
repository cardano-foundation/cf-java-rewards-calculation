package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncAccountDeregistration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashSet;

@Repository
@Profile("db-sync")
public interface DbSyncStakeDeregistrationRepository extends ReadOnlyRepository<DbSyncAccountDeregistration, Long>{

    @Query(nativeQuery = true, value = """
            SELECT
                sa.view AS stakeAddress
            FROM
                stake_deregistration sd
            JOIN
                stake_registration sr ON sd.addr_id = sr.addr_id AND sr.epoch_no <= :epoch
            JOIN
                tx tx1 ON tx1.id = sd.tx_id
            JOIN
                tx tx2 ON tx2.id = sr.tx_id
            JOIN
                block block1 ON block1.id = tx1.block_id AND (block1.epoch_no < :epoch OR (
                                                              block1.epoch_no = :epoch AND
                                                              block1.epoch_slot_no < :stabilityWindow))
            JOIN
                block block2 ON block2.id = tx2.block_id AND (block2.epoch_no < :epoch OR (
                                                              block2.epoch_no = :epoch AND
                                                              block2.epoch_slot_no < :stabilityWindow))
            JOIN
                stake_address sa ON sa.id = sd.addr_id
            WHERE
                sd.epoch_no <= :epoch
            GROUP BY
                sa.view
            HAVING
                MAX(sd.tx_id) > MAX(sr.tx_id)
            """)
    HashSet<String> getAccountDeregistrationsInEpoch(@Param("epoch") Integer epoch,
                                                     @Param("stabilityWindow") Long stabilityWindow);
}
