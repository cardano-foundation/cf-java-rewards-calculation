package org.cardanofoundation.rewards.reward;

import java.math.BigInteger;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import org.cardanofoundation.rewards.projection.PoolConfigProjection;

@Builder
@Getter
@Setter
public class PoolConfigProjectionTest implements PoolConfigProjection {
  Long poolId;
  BigInteger pledge;
  Double margin;
  BigInteger fixedCost;
  Long rewardAddressId;
  Integer certIndex;
  Long txId;
  Long poolUpdateId;
  Integer activateEpochNo;
}
