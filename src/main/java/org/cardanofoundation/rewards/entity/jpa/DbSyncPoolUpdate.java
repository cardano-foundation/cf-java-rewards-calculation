package org.cardanofoundation.rewards.entity.jpa;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

import java.util.HashSet;
import java.util.Set;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "pool_update")
public class DbSyncPoolUpdate {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hash_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncPoolHash pool;

    private Double pledge;
    private Double margin;
    @Column(name = "active_epoch_no")
    private Long activeEpochNumber;

    @Column(name = "fixed_cost")
    private Double fixedCost;
    @Column(name = "registered_tx_id")
    private Long registeredTxId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reward_addr_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncStakeAddress stakeAddress;
}
