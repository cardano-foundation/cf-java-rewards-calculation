package org.cardanofoundation.rewards.entity.jpa;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "pool_owner")
public class DbSyncPoolOwner {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "addr_id")
    private DbSyncStakeAddress stakeAddress;

    @Column(name = "pool_update_id")
    private Long poolUpdateId;
}
