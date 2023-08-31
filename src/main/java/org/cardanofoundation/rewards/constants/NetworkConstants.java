package org.cardanofoundation.rewards.constants;

import java.util.Map;

public class NetworkConstants {

  private NetworkConstants() {}

  public static final int TESTNET = 1097911063;
  public static final int PREPROD_TESTNET = 1;
  public static final int PREVIEW_TESTNET = 2;
  public static final int MAINNET = 764824073;

  public static final String MAINNET_NAME = "mainnet";
  public static final String PREPROD_NAME = "preprod";
  public static final String PREVIEW_NAME = "preview";
  public static final String GUILDNET_NAME = "guildnet";

  private static final Map<String, Integer> mNetworkNameAndMagic =
      Map.ofEntries(
          Map.entry(MAINNET_NAME, MAINNET),
          Map.entry(PREPROD_NAME, PREPROD_TESTNET),
          Map.entry(PREVIEW_NAME, PREVIEW_TESTNET));

  private static final Map<Integer, String> mNetworkMagicAndName =
      Map.ofEntries(
          Map.entry(MAINNET, MAINNET_NAME),
          Map.entry(PREPROD_TESTNET, PREPROD_NAME),
          Map.entry(PREVIEW_TESTNET, PREVIEW_NAME));

  public static int getNetworkMagicByName(String networkName) {
    return mNetworkNameAndMagic.get(networkName);
  }

  public static String getNetworkNameByMagicNumber(int magicNumber) {
    return mNetworkMagicAndName.get(magicNumber);
  }
}
