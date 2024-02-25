package org.cardanofoundation.rewards.calculation.domain;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Builder
@Getter
@Setter
public class EpochCalculationResult {
    int epoch;
    List<PoolRewardCalculationResult> PoolRewardCalculationResults;
    TreasuryCalculationResult treasuryCalculationResult;
    BigInteger totalAdaInCirculation;
    BigInteger treasury;
    BigInteger reserves;
    BigInteger totalDistributedRewards;
    BigInteger totalUndistributedRewards;
    BigInteger totalRewardsPot;
    BigInteger totalPoolRewardsPot;
}
