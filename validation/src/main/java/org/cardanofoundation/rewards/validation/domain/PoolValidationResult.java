package org.cardanofoundation.rewards.validation.domain;

import lombok.*;
import org.cardanofoundation.rewards.calculation.domain.PoolRewardCalculationResult;
import java.math.BigInteger;
import java.util.HashSet;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PoolValidationResult {
    private PoolRewardCalculationResult poolRewardCalculationResult;
    private HashSet<RewardValidation> rewardValidations;
    private BigInteger offset;
    private BigInteger calculatedTotalPoolRewards;
    private BigInteger expectedTotalPoolRewards;
    private boolean isValid;

    public String getPoolId() {
        return poolRewardCalculationResult.getPoolId();
    }

    public int getEpoch() {
        return poolRewardCalculationResult.getEpoch();
    }

    public String getRewardAddress() {
        return poolRewardCalculationResult.getRewardAddress();
    }

    public static PoolValidationResult from(PoolRewardCalculationResult poolRewardCalculationResult, HashSet<RewardValidation> rewardValidations) {
        BigInteger calculatedTotalPoolRewards = rewardValidations.stream()
                .map(RewardValidation::getCalculatedReward)
                .reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger expectedTotalPoolRewards = rewardValidations.stream()
                .map(RewardValidation::getExpectedReward)
                .reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger offset = calculatedTotalPoolRewards.subtract(expectedTotalPoolRewards).abs();
        boolean isValid = offset.equals(BigInteger.ZERO);
        return new PoolValidationResult(poolRewardCalculationResult, rewardValidations, offset, calculatedTotalPoolRewards, expectedTotalPoolRewards, isValid);
    }
}
