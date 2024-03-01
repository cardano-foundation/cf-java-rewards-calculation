package org.cardanofoundation.rewards.validation.entity.jpa;

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
@Table(name = "epoch_stake")
public class DbSyncEpochStake {
    @Id
    private Long id;

    @Column(name = "amount")
    private BigInteger amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addr_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncStakeAddress stakeAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncPoolHash pool;

    @Column(name = "epoch_no")
    private Integer epoch;
}
