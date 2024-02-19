package org.cardanofoundation.rewards.calculation.entity;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reward {
    private BigInteger amount;
    private String stakeAddress;
    private String poolId;
}
