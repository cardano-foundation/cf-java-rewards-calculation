package org.cardanofoundation.rewards.service;

import java.util.List;

import org.cardanofoundation.rewards.projection.PoolConfigProjection;

public interface PoolUpdateService {

  List<PoolConfigProjection> findAllActivePoolConfig(int epoch);

  List<PoolConfigProjection> findPoolHasMintedBlockInEpoch(int epoch);
}
