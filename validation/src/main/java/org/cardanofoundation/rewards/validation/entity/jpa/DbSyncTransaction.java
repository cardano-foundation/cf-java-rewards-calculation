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
@Table(name = "tx")
public class DbSyncTransaction {
    @Id
    private Long id;

    @ManyToOne
    @JoinColumn(name = "block_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncBlock block;

    private BigInteger deposit;

    private BigInteger fee;
}
