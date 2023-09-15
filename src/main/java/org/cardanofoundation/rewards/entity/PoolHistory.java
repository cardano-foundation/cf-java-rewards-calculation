package org.cardanofoundation.rewards.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolHistory {
    private Double activeStake;
    private Double delegatorRewards;
    private Double poolFees;
    private int blockCount;
    private int epoch;
}
