package org.cardanofoundation.rewards.projection;

import java.math.BigInteger;

public interface PoolConfigProjection {

  Long getPoolUpdateId();

  Long getPoolId();

  BigInteger getPledge();

  Double getMargin();

  BigInteger getFixedCost();

  Long getRewardAddressId();

  Integer getCertIndex();

  Long getTxId();

  Integer getActivateEpochNo();
}
