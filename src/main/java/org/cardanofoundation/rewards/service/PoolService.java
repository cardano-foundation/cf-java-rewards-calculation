package org.cardanofoundation.rewards.service;

import org.cardanofoundation.rewards.common.entity.EpochStake;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PoolService {

  Set<Long> getPoolCanStakeFromEpoch(int epoch);

  Map<Long, BigDecimal> getPoolPerformanceInEpoch(int epochNo, List<EpochStake> epochStakes);

  Map<Long, BigDecimal> getEstimatedBlockOfPoolInEpoch(int epochNo, List<EpochStake> epochStakes);
}
