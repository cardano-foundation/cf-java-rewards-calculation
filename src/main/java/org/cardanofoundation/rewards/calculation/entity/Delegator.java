package org.cardanofoundation.rewards.calculation.entity;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delegator {
    String stakeAddress;
    BigInteger activeStake;
}
