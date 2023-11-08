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
@Table(name = "stake_deregistration")
public class DbSyncAccountDeregistration {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "addr_id")
    private DbSyncStakeAddress address;

    @Column(name = "epoch_no")
    private Integer epoch;

    @ManyToOne
    @JoinColumn(name = "tx_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncTransaction transaction;
}
