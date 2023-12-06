package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncAccountDeregistration;
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

    @Query("SELECT deregistration.address.view AS address, 'DEREGISTRATION' AS action, " +
            "deregistration.transaction.block.time AS unixBlockTime " +
            "FROM DbSyncAccountDeregistration deregistration WHERE " +
            "deregistration.epoch <= :epoch AND deregistration.epoch > :epoch-2")
    List<StakeAccountUpdate> getRecentAccountDeregistrationsBeforeEpoch(Integer epoch);

    @Query("SELECT COUNT(*) FROM DbSyncAccountDeregistration WHERE epoch = :epoch")
    Integer countDeregistrationsInEpoch(Integer epoch);
}
