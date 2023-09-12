package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class PoolCalculationResult {
    BigDecimal expectedPoolReward;
    BigDecimal actualPoolReward;
    BigDecimal totalRewardPot;
    BigDecimal stakePoolRewardsPot;
    BigDecimal poolFee;
    int optimalPoolCount;
    double influenceParam;
    BigDecimal relativeStakeOfPool;
    BigDecimal relativeStakeOfPoolOwner;
    BigDecimal poolPerformance;

    public BigDecimal getActualPoolRewardWithFee() {
        return this.actualPoolReward.add(this.poolFee);
    }
}
