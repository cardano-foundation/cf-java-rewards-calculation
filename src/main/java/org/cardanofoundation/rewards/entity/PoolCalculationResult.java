package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Builder
@Getter
public class PoolCalculationResult {
    Double expectedPoolReward;
    Double actualPoolReward;
    Double totalRewardPot;
    Double stakePoolRewardsPot;
    Double poolFee;
    int optimalPoolCount;
    Double influenceParam;
    Double relativeStakeOfPool;
    Double relativeStakeOfPoolOwner;
    Double poolPerformance;

    public Double getActualPoolRewardWithFee() {
        return this.actualPoolReward + this.poolFee;
    }
}
