package org.cardanofoundation.rewards.validation.mapper;
import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncReward;
import org.cardanofoundation.rewards.calculation.domain.Reward;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.MemberReward;

public class RewardMapper {

    public static Reward fromDbSyncReward(DbSyncReward reward) {
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
