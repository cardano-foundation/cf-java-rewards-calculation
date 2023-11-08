package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncAccountDeregistration;
import org.cardanofoundation.rewards.entity.jpa.DbSyncAccountRegistration;
import org.cardanofoundation.rewards.entity.jpa.DbSyncStakeAddress;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncStakeRegistrationRepository extends ReadOnlyRepository<DbSyncAccountRegistration, Long>{
    @Query("SELECT registration FROM DbSyncAccountRegistration registration WHERE " +
            "registration.address.view IN :addresses AND registration.epoch <= :epoch " +
            "ORDER BY registration.transaction.id DESC")
    List<DbSyncAccountRegistration> getLatestAccountRegistrationsUntilEpochForAddresses(
            List<String> addresses, Integer epoch);
}
