package org.cardanofoundation.rewards.entity.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "epoch")
public class DbSyncEpoch {
    @Id
    private Long id;

    @Column(name = "out_sum")
    private BigInteger output;

    @Column(name = "fees")
    private BigInteger fees;

    @Column(name = "blk_count")
    private Integer blockCount;

    @Column(name = "no")
    private Integer number;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;
}
