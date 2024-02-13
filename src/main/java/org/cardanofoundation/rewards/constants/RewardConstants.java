package org.cardanofoundation.rewards.constants;

public class RewardConstants {
  public static final double TOTAL_LOVELACE = 45000000000000000.0;
  public static final double DEPOSIT_POOL_REGISTRATION_IN_LOVELACE = 500000000.0;
  public static final double DEPOSIT_ACCOUNT_REGISTRATION_IN_LOVELACE = 2000000.0;
  public static final int DEPOSIT_POOL_REGISTRATION_IN_ADA = 500;
  // https://developers.cardano.org/docs/operate-a-stake-pool/introduction-to-cardano/#slots-and-epochs
  public static final int EXPECTED_SLOT_PER_EPOCH = 432000;
  public static final int EXPECTED_BLOCKS_PER_EPOCH = 21600;
}
