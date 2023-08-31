package org.cardanofoundation.rewards.common;

import java.math.BigInteger;

import lombok.*;
import lombok.experimental.FieldDefaults;

import org.cardanofoundation.rewards.common.entity.RewardType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StakingReward {
  String stakeAddress;
  String poolId;
  BigInteger amount;
  RewardType rewardType;
  int earnedEpoch;
  int spendableEpoch;
}
