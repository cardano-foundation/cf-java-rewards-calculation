package org.cardanofoundation.rewards.service;

import org.cardanofoundation.rewards.common.EpochEndData;
import rest.koios.client.backend.api.base.exception.ApiException;

public interface EpochEndService {
  EpochEndData getEpochEndData(int epoch) throws ApiException;

  void saveEpochEndData(EpochEndData epochEndData);
}
