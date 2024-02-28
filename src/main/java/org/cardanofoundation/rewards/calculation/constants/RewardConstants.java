package org.cardanofoundation.rewards.calculation.constants;

import java.math.BigInteger;

public class RewardConstants {
  public static final BigInteger TOTAL_LOVELACE = new BigInteger("45000000000000000");
  public static final BigInteger POOL_DEPOSIT_IN_LOVELACE = BigInteger.valueOf(500000000);
  // https://developers.cardano.org/docs/operate-a-stake-pool/introduction-to-cardano/#slots-and-epochs
  public static final int EXPECTED_SLOT_PER_EPOCH = 432000;
  public static final BigInteger MAINNET_SHELLEY_INITIAL_RESERVES = new BigInteger("13888022852926644");
  public static final BigInteger MAINNET_SHELLEY_INITIAL_TREASURY = new BigInteger("0");
  public static final BigInteger MAINNET_SHELLEY_INITIAL_UTXO = TOTAL_LOVELACE.subtract(MAINNET_SHELLEY_INITIAL_RESERVES);
  public static final int GENESIS_CONFIG_SECURITY_PARAMETER = 2160;
  public static final int MAINNET_SHELLEY_START_EPOCH = 208;
  public static final int MAINNET_ALLEGRA_HARDFORK_EPOCH = 236;
  public static final int MAINNET_VASIL_HARDFORK_EPOCH = 350;
  public static final BigInteger MAINNET_BOOTSTRAP_ADDRESS_AMOUNT = new BigInteger("318200635000000");
  public static final double ACTIVE_SLOT_COEFFICIENT = 0.05;
  public static final long RANDOMNESS_STABILISATION_WINDOW = Math.round(
          (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT);
}
