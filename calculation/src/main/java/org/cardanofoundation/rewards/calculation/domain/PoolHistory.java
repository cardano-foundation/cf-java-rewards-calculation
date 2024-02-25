package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolHistory {
    private String poolId;
    private BigInteger activeStake;
    private BigInteger delegatorRewards;
    private String rewardAddress;
    private List<String> owners;
    private BigInteger ownerActiveStake;
    private BigInteger poolFees;
    private Double margin;
    private BigInteger fixedCost;
    private BigInteger pledge;
    private List<Delegator> delegators;
    private int blockCount;
    private int epoch;
}
