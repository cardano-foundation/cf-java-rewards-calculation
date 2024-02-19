package org.cardanofoundation.rewards.mapper;
import org.cardanofoundation.rewards.entity.Reward;
import org.cardanofoundation.rewards.entity.jpa.projection.MemberReward;
import org.cardanofoundation.rewards.entity.jpa.projection.TotalPoolRewards;

import java.util.List;

public class RewardMapper {

    public static Reward fromDbSyncReward(org.cardanofoundation.rewards.entity.jpa.DbSyncReward reward) {
        if (reward == null) return null;

        return Reward.builder()
                .amount(reward.getAmount())
                .stakeAddress(reward.getStakeAddress().getView())
                .build();
    }

    public static Reward fromMemberReward(MemberReward memberReward) {
        if (memberReward == null) return null;

        return Reward.builder()
                .amount(memberReward.getAmount())
                .stakeAddress(memberReward.getStakeAddress())
                .poolId(memberReward.getPoolId())
                .build();
    }
}
