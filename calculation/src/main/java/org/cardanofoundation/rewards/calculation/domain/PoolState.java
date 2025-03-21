package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolState {
    private String poolId;
    private BigInteger activeStake;
    private String rewardAddress;
    private HashSet<String> owners;
    private BigInteger ownerActiveStake;
    private BigInteger poolFees;
    private BigDecimal margin;
    private BigInteger fixedCost;
    private BigInteger pledge;
    private HashSet<Delegator> delegators;
    private int blockCount;
    private int epoch;
}
