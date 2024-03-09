package org.cardanofoundation.rewards.validation.entity.dbsync;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

import java.util.Date;

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

    @ManyToOne
    @JoinColumn(name = "slot_leader_id")
    private DbSyncSlotLeader slotLeader;

    private Date time;
}
