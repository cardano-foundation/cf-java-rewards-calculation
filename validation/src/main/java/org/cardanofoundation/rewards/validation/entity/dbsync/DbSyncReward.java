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
@Table(name = "reward")
public class DbSyncReward {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pool_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncPoolHash pool;

    private BigInteger amount;
    private String type;

    @ManyToOne
    @JoinColumn(name = "addr_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncStakeAddress stakeAddress;

    @Column(name = "earned_epoch")
    private Long earnedEpoch;
    @Column(name = "spendable_epoch")
    private Long spendableEpoch;
}
