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
@Table(name = "pool_retire")
public class DbSyncPoolRetirement {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hash_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncPoolHash pool;

    @Column(name = "retiring_epoch")
    private Integer retiringEpoch;

    @ManyToOne
    @JoinColumn(name = "announced_tx_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncTransaction announcedTransaction;
}
