package org.cardanofoundation.rewards.constants;

public class RewardConstants {
  public static final long TOTAL_LOVELACE = 45000000000000000L;
  public static final long DEPOSIT_POOL_REGISTRATION_IN_LOVELACE = 500000000L;
  public static final int DEPOSIT_POOL_REGISTRATION_IN_ADA = 500;
  // https://developers.cardano.org/docs/operate-a-stake-pool/introduction-to-cardano/#slots-and-epochs
  public static final int EXPECTED_SLOT_PER_EPOCH = 432000;
  public static final int EXPECTED_BLOCKS_PER_EPOCH = 21600;
}
