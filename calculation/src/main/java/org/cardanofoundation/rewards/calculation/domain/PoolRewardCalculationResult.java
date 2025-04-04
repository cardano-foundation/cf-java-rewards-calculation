package org.cardanofoundation.rewards.calculation.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Builder
@Getter
@Setter
public class PoolRewardCalculationResult {
    int epoch;
    String poolId;
    String rewardAddress;
    BigInteger stakePoolRewardsPot;
    HashSet<Reward> memberRewards;
    BigInteger operatorReward;
    BigInteger optimalPoolReward;
    BigInteger poolReward;
    BigDecimal apparentPoolPerformance;
    BigInteger poolFee;
    BigInteger distributedPoolReward;
    BigDecimal poolMargin;
    BigInteger poolCost;
    Set<String> poolOwnerStakeAddresses;
    BigInteger unspendableEarnedRewards;
}
