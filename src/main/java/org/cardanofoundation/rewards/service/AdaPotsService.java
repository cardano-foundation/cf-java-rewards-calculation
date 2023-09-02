package org.cardanofoundation.rewards.service;

import org.cardanofoundation.rewards.common.entity.AdaPots;
import org.cardanofoundation.rewards.common.entity.EpochParam;
import org.cardanofoundation.rewards.exception.PreviousAdaPotsNotFoundException;
import org.cardanofoundation.rewards.exception.RefundConflictException;
import rest.koios.client.backend.api.base.exception.ApiException;

import org.cardanofoundation.rewards.common.PoolRetiredReward;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface AdaPotsService {
  BigInteger calculateReserves(int epochNo);

  BigDecimal calculateTreasury(double treasuryGrowRate, BigDecimal rewardPot, BigDecimal lastTreasury);

  BigDecimal calculateRewards(double monetaryExpandRate, BigDecimal reserve, BigDecimal fee);

  BigInteger calculateUTXO(int epochNo);

  BigInteger calculateDeposits(int epochNo);

  BigInteger calculateFees(int epochNo);
}
