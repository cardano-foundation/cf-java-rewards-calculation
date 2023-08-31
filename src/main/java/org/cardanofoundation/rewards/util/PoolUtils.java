package org.cardanofoundation.rewards.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class PoolUtils {

  public static BigInteger calculatePoolFee(
      BigInteger poolReward, BigInteger fixedCost, Double margin) {
    if (poolReward.compareTo(fixedCost) <= 0) {
      return poolReward;
    }
    var marginFee =
        new BigDecimal(poolReward)
            .subtract(new BigDecimal(fixedCost))
            .multiply(new BigDecimal(margin));
    return marginFee.add(new BigDecimal(fixedCost)).toBigInteger();
  }
}
