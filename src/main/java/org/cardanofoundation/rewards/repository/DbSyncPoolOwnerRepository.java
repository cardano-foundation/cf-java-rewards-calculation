package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncPoolOwner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncPoolOwnerRepository extends ReadOnlyRepository<DbSyncPoolOwner, Long> {

    @Query("""
            SELECT owner FROM DbSyncPoolOwner AS owner
                WHERE owner.poolUpdateId = :poolUpdateId""")
    List<DbSyncPoolOwner> getByPoolUpdateId(Long poolUpdateId);
}
