package org.cardanofoundation.rewards.validation.entity.dbsync;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

import java.math.BigInteger;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "ada_pots")
public class DbSyncAdaPots {
    @Id
    private Long id;

    @Column(name = "slot_no")
    private Long slot;

    @Column(name = "epoch_no")
    private Integer epoch;

    @Column(name = "treasury")
    private BigInteger treasury;

    @Column(name = "reserves")
    private BigInteger reserves;

    @Column(name = "rewards")
    private BigInteger rewards;

    @Column(name = "utxo")
    private BigInteger utxo;

    @Column(name = "deposits")
    private BigInteger deposits;

    @Column(name = "fees")
    private BigInteger fees;

    @Column(name = "block_id")
    private Long blockId;
}
