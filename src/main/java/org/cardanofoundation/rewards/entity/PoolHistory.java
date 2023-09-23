package org.cardanofoundation.rewards.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolHistory {
    private Double activeStake;
    private Double delegatorRewards;
    private Double poolFees;
    private Double margin;
    private Double fixedCost;
    private List<Delegator> delegators;
    private int blockCount;
    private int epoch;
}
