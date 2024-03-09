package org.cardanofoundation.rewards.validation.domain;

import lombok.*;
import org.cardanofoundation.rewards.validation.entity.projection.TotalPoolRewards;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolReward {
    private String poolId;
    private int epoch;
    private BigInteger amount;

    public static PoolReward fromTotalPoolRewards(TotalPoolRewards totalPoolRewards, int epoch) {
        if (totalPoolRewards == null) {
            return null;
        }
        return PoolReward.builder()
                .poolId(totalPoolRewards.getPoolId())
                .epoch(epoch)
                .amount(totalPoolRewards.getAmount())
                .build();
    }
}
