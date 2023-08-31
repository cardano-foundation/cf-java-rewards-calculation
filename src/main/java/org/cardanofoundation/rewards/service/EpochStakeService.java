package org.cardanofoundation.rewards.service;

import java.util.Collection;
import java.util.List;

import org.cardanofoundation.rewards.common.entity.EpochStake;

public interface EpochStakeService {
  Collection<EpochStake> calculateEpochStakeOfEpoch(int epochNo);

  void removeCacheEpochStakeByEpochNo(int epoch);

  List<EpochStake> getAllEpochStakeByEpochNo(int epoch);
}
