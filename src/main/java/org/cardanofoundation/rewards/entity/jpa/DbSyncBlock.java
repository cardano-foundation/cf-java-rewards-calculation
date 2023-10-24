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
@Table(name = "block")
public class DbSyncBlock {
    @Id
    private Long id;
    @Column(name = "block_no")
    private Integer blockNo;
    @Column(name = "epoch_no")
    private Integer epochNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "slot_leader_id", nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT, name = "none"))
    @EqualsAndHashCode.Exclude
    private DbSyncSlotLeader slotLeader;
}
