package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncAccountDeregistration;
import org.cardanofoundation.rewards.entity.jpa.projection.LatestStakeAccountUpdate;
import org.cardanofoundation.rewards.entity.jpa.projection.StakeAccountUpdate;
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
            WITH latest_registration AS (
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
                    WHEN lr.tx_id > ld.tx_id THEN 'REGISTRATION'
                    WHEN lr.tx_id < ld.tx_id THEN 'DEREGISTRATION'
                    ELSE 'REGISTRATION'
                END AS latestUpdateType
            FROM
                latest_registration lr
            FULL OUTER JOIN
                latest_deregistration ld ON lr.addr_id = ld.addr_id
            JOIN
                stake_address ON stake_address.id = lr.addr_id
            WHERE
                stake_address.view IN :stakeAddresses
        """)
    List<LatestStakeAccountUpdate> getLatestStakeAccountUpdates(Integer epoch, List<String> stakeAddresses);

    @Query("SELECT deregistration.address.view AS address, 'DEREGISTRATION' AS action, " +
            "deregistration.transaction.block.time AS unixBlockTime " +
            "FROM DbSyncAccountDeregistration deregistration WHERE " +
            "deregistration.epoch <= :epoch AND deregistration.epoch > :epoch-2")
    List<StakeAccountUpdate> getRecentAccountDeregistrationsBeforeEpoch(Integer epoch);

    @Query("SELECT COUNT(*) FROM DbSyncAccountDeregistration WHERE epoch = :epoch")
    Integer countDeregistrationsInEpoch(Integer epoch);
}
