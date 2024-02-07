package org.cardanofoundation.rewards.mapper;
import org.cardanofoundation.rewards.entity.Reward;

public class RewardMapper {

    public static Reward fromDbSyncReward(org.cardanofoundation.rewards.entity.jpa.DbSyncReward reward) {
        if (reward == null) return null;

        return Reward.builder()
                .amount(reward.getAmount())
                .stakeAddress(reward.getStakeAddress().getView())
                .build();
    }
}
