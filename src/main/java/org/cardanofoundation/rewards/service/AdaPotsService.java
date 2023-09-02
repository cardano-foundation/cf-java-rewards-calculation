package org.cardanofoundation.rewards.service;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface AdaPotsService {
  BigInteger calculateReserves(int epochNo);

  BigDecimal calculateTreasury(double treasuryGrowRate, BigDecimal rewardPot, BigDecimal lastTreasury);

  BigDecimal calculateTotalRewardPot(double monetaryExpandRate, BigDecimal reserve, BigDecimal fee);

  BigInteger calculateUTXO(int epochNo);

  BigInteger calculateDeposits(int epochNo);

  BigInteger calculateFees(int epochNo);
}
