package org.cardanofoundation.rewards.common;

import java.util.Objects;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.cardanofoundation.rewards.common.entity.RewardType;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MissingReward {
  String stakeAddress;
  Long stakeAddressId;
  String poolHash;
  Long poolId;
  RewardType rewardType;
  int earnedEpoch;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MissingReward that = (MissingReward) o;
    return Objects.equals(stakeAddressId, that.stakeAddressId)
        && Objects.equals(poolId, that.poolId)
        && rewardType == that.rewardType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(stakeAddressId, poolId, rewardType);
  }
}
