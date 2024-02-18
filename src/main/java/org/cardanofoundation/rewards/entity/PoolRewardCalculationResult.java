package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Builder
@Getter
@Setter
public class PoolRewardCalculationResult {
    int epoch;
    String poolId;
    String rewardAddress;
    BigInteger stakePoolRewardsPot;
    List<Reward> memberRewards;
    BigInteger operatorReward;
    BigInteger optimalPoolReward;
    BigInteger poolReward;
    BigDecimal apparentPoolPerformance;
    BigInteger poolFee;
    BigInteger distributedPoolReward;
    Double poolMargin;
    BigInteger poolCost;
    List<String> poolOwnerStakeAddresses;
}
