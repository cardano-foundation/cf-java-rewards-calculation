package org.cardanofoundation.rewards.service;

import java.util.Set;

import org.cardanofoundation.rewards.common.PoolRetiredReward;

public interface PoolRetireService {

  PoolRetiredReward getRefundRewards(int epochNo);

  Set<Long> getPoolRetiredIdTilEpoch(int epoch);
}
