package org.cardanofoundation.rewards.entity.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "epoch_param")
public class DbSyncProtocolParameters {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "epoch_no")
    Integer epoch;

    @Column(name = "decentralisation")
    Double decentralisation;

    @Column(name = "treasury_growth_rate")
    Double treasuryGrowRate;

    @Column(name = "monetary_expand_rate")
    Double monetaryExpandRate;

    @Column(name = "optimal_pool_count")
    int optimalPoolCount;

    @Column(name = "influence")
    Double poolOwnerInfluence;
}
