package org.cardanofoundation.rewards.entity;

import lombok.*;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolHistory {
    private BigInteger activeStake;
    private BigInteger delegatorRewards;
    private String rewardAddress;
    private BigInteger poolFees;
    private Double margin;
    private Double fixedCost;
    private List<Delegator> delegators;
    private int blockCount;
    private int epoch;

    public Delegator getDelegator(String stakeAddress) {
        for (Delegator delegator : delegators) {
            if (delegator.getStakeAddress().equals(stakeAddress)) {
                return delegator;
            }
        }
        return null;
    }
}
