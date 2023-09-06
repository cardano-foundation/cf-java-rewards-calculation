package org.cardanofoundation.rewards.constants;

import java.math.BigDecimal;
import java.math.BigInteger;

public class RewardConstants {
  public static final BigDecimal TOTAL_LOVELACE = new BigDecimal("45000000000000000");
  public static final BigInteger DEPOSIT_POOL_REGISTRATION_IN_LOVELACE = BigInteger.valueOf(500000000L);
  // https://developers.cardano.org/docs/operate-a-stake-pool/introduction-to-cardano/#slots-and-epochs
  public static final int EXPECTED_SLOT_PER_EPOCH = 432000;
}
