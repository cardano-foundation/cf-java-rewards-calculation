package org.cardanofoundation.rewards.entity.jpa;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.springframework.context.annotation.Profile;

@Entity
@Immutable
@Getter
@Profile("db-sync")
@Table(name = "stake_address")
public class DbSyncStakeAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String view;
}
