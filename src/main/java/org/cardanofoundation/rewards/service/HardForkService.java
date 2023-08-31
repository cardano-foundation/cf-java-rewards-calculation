package org.cardanofoundation.rewards.service;

import org.cardanofoundation.rewards.common.entity.Reward;

import java.util.Collection;

public interface HardForkService {

  Collection<Reward> handleRewardIssueForEachEpoch(
      int networkMagic, int epoch, Collection<Reward> poolStakeHistories);
}
