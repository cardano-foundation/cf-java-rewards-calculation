package org.cardanofoundation.rewards.service;

import java.math.BigInteger;
import java.util.Collection;

import org.cardanofoundation.rewards.common.entity.Reward;
import org.cardanofoundation.rewards.common.entity.RewardType;
import org.cardanofoundation.rewards.exception.StakeBalanceException;

public interface RewardService {

  BigInteger getTotalRemainRewardOfEpoch(int epoch);

  Collection<Reward> calculateReward(int epoch, BigInteger reserves, BigInteger fee)
      throws StakeBalanceException;

  BigInteger getTotalRewardOfEpochByType(int epoch, RewardType rewardType);

  BigInteger getDistributedReward(int epoch);
}
