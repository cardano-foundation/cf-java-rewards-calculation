package org.cardanofoundation.rewards.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.cardanofoundation.rewards.service.AdaPotsService;

@Slf4j
@Service
public class AdaPotsServiceImpl implements AdaPotsService {

  /*
   * Calculate the treasury for epoch e with the formula:
   *
   * treasury(e) = treasury_growth_rate * (reward_pot * 0.2) + treasury(e - 1)
   * treasury(e) = 0, if e < 209
   */
  public BigDecimal calculateTreasury(double treasuryGrowRate, BigDecimal rewardPot, BigDecimal lastTreasury) {
    BigDecimal treasury = rewardPot.multiply(new BigDecimal(treasuryGrowRate));
    return treasury.add(lastTreasury);
  }

  /*
   * Calculate the reward pot for epoch e with the formula:
   *
   * rewards(e) = monetary_expand_rate * reserve(e - 1) + fee(e - 1)
   * rewards(e) = 0, if e < 209
   */
  public BigDecimal calculateTotalRewardPot(double monetaryExpandRate, BigDecimal reserve, BigDecimal fee) {
    BigDecimal totalRewardPot = reserve.multiply(new BigDecimal(monetaryExpandRate));
    return totalRewardPot.add(fee);
  }

  @Override
  public BigInteger calculateReserves(int epochNo) {
    return null;
  }

  @Override
  public BigInteger calculateUTXO(int epochNo) {
    return null;
  }

  @Override
  public BigInteger calculateDeposits(int epochNo) {
    return null;
  }

  @Override
  public BigInteger calculateFees(int epochNo) {
    return null;
  }
}
