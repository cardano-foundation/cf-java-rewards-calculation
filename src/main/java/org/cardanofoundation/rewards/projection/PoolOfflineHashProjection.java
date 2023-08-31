package org.cardanofoundation.rewards.projection;

public interface PoolOfflineHashProjection {
  Long getPoolId();

  Long getPoolRefId();

  String getHash();
}
