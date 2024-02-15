package org.cardanofoundation.rewards.constants;

import java.math.BigInteger;

public class RewardConstants {
  public static final BigInteger TOTAL_LOVELACE = new BigInteger("45000000000000000");
  public static final BigInteger POOL_DEPOSIT_IN_LOVELACE = BigInteger.valueOf(500000000);
  // https://developers.cardano.org/docs/operate-a-stake-pool/introduction-to-cardano/#slots-and-epochs
  public static final int EXPECTED_SLOT_PER_EPOCH = 432000;
}
