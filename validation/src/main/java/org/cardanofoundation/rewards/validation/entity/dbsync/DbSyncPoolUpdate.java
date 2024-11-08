package org.cardanofoundation.rewards.validation.entity.dbsync;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

import java.math.BigInteger;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "pool_update")
public class DbSyncPoolUpdate {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hash_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncPoolHash pool;

    private BigInteger pledge;
    private Double margin;
    @Column(name = "active_epoch_no")
    private Long activeEpochNumber;

    @Column(name = "fixed_cost")
    private BigInteger fixedCost;

    @Column(name = "deposit")
    private BigInteger deposit;

    @ManyToOne
    @JoinColumn(name = "registered_tx_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncTransaction registeredTransaction;

    @ManyToOne
    @JoinColumn(name = "reward_addr_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncStakeAddress stakeAddress;
}
