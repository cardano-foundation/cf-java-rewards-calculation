package org.cardanofoundation.rewards.entity.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "pool_hash")
public class DbSyncPoolHash {
    @Id
    private Long id;

    @Column(name = "view")
    private String bech32PoolId;
}
