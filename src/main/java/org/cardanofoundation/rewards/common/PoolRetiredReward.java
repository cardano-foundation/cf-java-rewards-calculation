package org.cardanofoundation.rewards.common;

import java.math.BigInteger;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.cardanofoundation.rewards.common.entity.Reward;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolRetiredReward {

  BigInteger additionTreasury;

  List<Reward> refundRewards;
}
