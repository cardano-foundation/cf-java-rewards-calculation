package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PoolHistory {
    private Double activeStake;
    private Double delegatorRewards;
    private Double poolFees;
    private int blockCount;
    private int epoch;
}
