package org.cardanofoundation.rewards.validation.domain;

import lombok.*;
import org.cardanofoundation.rewards.calculation.domain.Reward;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpochValidationPoolReward {
    private String poolId;
    private BigInteger totalPoolReward;
    private HashSet<EpochValidationDelegatorReward> delegatorRewards;

    public void setDelegatorRewardsFromMemberRewards(HashSet<Reward> rewards) {
        this.delegatorRewards = rewards.stream()
                .map(reward -> EpochValidationDelegatorReward.builder()
                        .stakeAddress(reward.getStakeAddress())
                        .reward(reward.getAmount())
                        .build())
                .collect(Collectors.toCollection(HashSet::new));
    }
}
