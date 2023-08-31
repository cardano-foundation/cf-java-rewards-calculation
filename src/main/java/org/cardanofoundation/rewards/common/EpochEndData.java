package org.cardanofoundation.rewards.common;

import java.util.Collection;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.cardanofoundation.rewards.common.entity.AdaPots;
import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.common.entity.Reward;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EpochEndData {
  AdaPots adaPots;
  Collection<Reward> refunds;
  Collection<EpochStake> epochStakes;
  Collection<Reward> stakeRewards;
}
