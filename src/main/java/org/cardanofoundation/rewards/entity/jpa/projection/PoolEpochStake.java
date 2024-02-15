package org.cardanofoundation.rewards.entity.jpa.projection;

import java.math.BigInteger;

public interface PoolEpochStake {
    BigInteger getAmount();
    String getPoolId();
    String getStakeAddress();
    Integer getEpoch();
}
