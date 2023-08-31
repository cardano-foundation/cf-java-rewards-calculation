package org.cardanofoundation.rewards.service.impl;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import org.cardanofoundation.rewards.projection.PoolConfigProjection;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RewardParam {
  long stakeAddressId;
  BigDecimal relativeStake;
  BigDecimal poolReward;
  BigDecimal relativeStakeOfPool;
  PoolConfigProjection poolConfigProjection;
  int epochNo;
}
