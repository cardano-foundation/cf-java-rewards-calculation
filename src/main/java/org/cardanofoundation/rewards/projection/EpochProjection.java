package org.cardanofoundation.rewards.projection;

import java.math.BigInteger;

public interface EpochProjection {
  BigInteger getFee();

  BigInteger getEpochNo();
}
