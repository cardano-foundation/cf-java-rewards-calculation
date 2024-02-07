package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class PoolRewardCalculationResult {
    int epoch;
    String poolId;
    String rewardAddress;
    Double stakePoolRewardsPot;
    List<Reward> memberRewards;
    Double operatorReward;
    Double optimalPoolReward;
    Double poolReward;
    Double apparentPoolPerformance;
    Double poolFee;
    Double poolMargin;
    Double poolCost;
    List<String> poolOwnerStakeAddresses;
}
