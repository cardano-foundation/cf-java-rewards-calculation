package org.cardanofoundation.rewards.entity.jpa.projection;

public interface PoolEpochStake {
    Double getAmount();
    String getPoolId();
    String getStakeAddress();
    Integer getEpoch();
}
