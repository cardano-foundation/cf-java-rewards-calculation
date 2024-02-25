package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncAccountDeregistration;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.LatestStakeAccountUpdate;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncStakeDeregistrationRepository extends ReadOnlyRepository<DbSyncAccountDeregistration, Long>{
    @Query("SELECT deregistration FROM DbSyncAccountDeregistration deregistration WHERE " +
            "deregistration.address.view IN :addresses AND deregistration.epoch <= :epoch " +
            "ORDER BY deregistration.transaction.id DESC")
    List<DbSyncAccountDeregistration> getLatestAccountDeregistrationsUntilEpochForAddresses(
            List<String> addresses, Integer epoch);

    @Query(nativeQuery = true, value = """
SELECT stakeAddress, latestUpdateType, block.epoch_slot_no AS epochSlot, block.epoch_no AS epoch FROM (WITH latest_registration AS (
                SELECT
                    addr_id,
                    MAX(tx_id) AS tx_id
                FROM
                    stake_registration
                WHERE
                    epoch_no <= :epoch
                GROUP BY
                    addr_id
            ),
            latest_deregistration AS (
                SELECT
                    addr_id,
                    MAX(tx_id) AS tx_id
                FROM
                    stake_deregistration
                WHERE
                    epoch_no <= :epoch
                GROUP BY
                    addr_id
            )
            SELECT
                stake_address.view AS stakeAddress,
                CASE
                    WHEN ld.tx_id IS NULL THEN 'REGISTRATION'
                    WHEN lr.tx_id >= ld.tx_id THEN 'REGISTRATION'
                    WHEN lr.tx_id < ld.tx_id THEN 'DEREGISTRATION'
                END AS latestUpdateType,
				CASE
					WHEN ld.tx_id IS NULL THEN lr.tx_id
                    WHEN lr.tx_id >= ld.tx_id THEN lr.tx_id
                    WHEN lr.tx_id < ld.tx_id THEN ld.tx_id
                END AS tx_id
            FROM
                latest_registration lr
            FULL OUTER JOIN
                latest_deregistration ld ON lr.addr_id = ld.addr_id
            JOIN
                stake_address ON stake_address.id = lr.addr_id )
    AS latest_update JOIN tx ON tx.id=latest_update.tx_id JOIN block ON block.id = tx.block_id""")
    List<LatestStakeAccountUpdate> getLatestStakeAccountUpdates(Integer epoch);

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
    List<String> getAccountDeregistrationsInEpoch(Integer epoch, Long stabilityWindow);

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
            	block block1 ON block1.id = tx1.block_id AND block1.epoch_no = :epoch AND block1.epoch_slot_no > :stabilityWindow
            JOIN
            	block block2 ON block2.id = tx2.block_id AND block2.epoch_no <= :epoch OR (
            		block2.epoch_no = :epoch AND block2.epoch_slot_no > :stabilityWindow)
            JOIN
            	stake_address sa ON sa.id = sd.addr_id
            WHERE
            	sd.epoch_no = :epoch
            GROUP BY
            	sa.view
            HAVING
            	MAX(sd.tx_id) > MAX(sr.tx_id);
            """)
    List<String> getLateAccountDeregistrationsInEpoch(Integer epoch, Long stabilityWindow);
}
