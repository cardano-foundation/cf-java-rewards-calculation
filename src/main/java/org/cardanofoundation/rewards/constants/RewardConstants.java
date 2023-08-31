package org.cardanofoundation.rewards.constants;

import java.math.BigInteger;

public class RewardConstants {

  public static final int BATCH_QUERY_SIZE = 3000;
  public static final BigInteger TOTAL_ADA = BigInteger.valueOf(45000000000000000L);
  public static final BigInteger REFUND_ADA = BigInteger.valueOf(500000000L);

  public static final int MAINNET_NETWORK_MAGIC = 764824073;

  public static final double ADJUSTMENT_POOL_PERFORMANCE_DECENTRALISATION = 0.8;

  public static final int EXPECTED_SLOT_PER_EPOCH = 21600;

  // 1 epoch last 5 days
  // epoch per year = 365 / 5 = 73;
  public static int EPOCH_PER_YEAR = 73;
}
