package org.cardanofoundation.rewards.calculation.entity;

import lombok.*;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PoolOwnerHistory {
    int epoch;
    List<String> stakeAddresses;
    BigInteger activeStake;
}
