package org.cardanofoundation.rewards.entity.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "ada_pots")
public class DbSyncAdaPots {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_no")
    private Long slot;

    @Column(name = "epoch_no")
    private Integer epoch;

    @Column(name = "treasury")
    private Double treasury;

    @Column(name = "reserves")
    private Double reserves;

    @Column(name = "rewards")
    private Double rewards;

    @Column(name = "utxo")
    private Double utxo;

    @Column(name = "deposits")
    private Double deposits;

    @Column(name = "fees")
    private Double fees;

    @Column(name = "block_id")
    private Long blockId;
}
