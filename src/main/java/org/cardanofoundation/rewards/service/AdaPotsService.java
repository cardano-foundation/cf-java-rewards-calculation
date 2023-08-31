package org.cardanofoundation.rewards.service;

import org.cardanofoundation.rewards.common.entity.AdaPots;
import org.cardanofoundation.rewards.exception.PreviousAdaPotsNotFoundException;
import org.cardanofoundation.rewards.exception.RefundConflictException;
import rest.koios.client.backend.api.base.exception.ApiException;

import org.cardanofoundation.rewards.common.PoolRetiredReward;

public interface AdaPotsService {
  AdaPots calculateAdaPots(int epochNo) throws PreviousAdaPotsNotFoundException;

  void save(AdaPots adaPots);

  int getLastEpochHasUpdated();

  void updateRefundDeposit(AdaPots adaPots, PoolRetiredReward poolRetiredReward)
      throws RefundConflictException, ApiException;
}
