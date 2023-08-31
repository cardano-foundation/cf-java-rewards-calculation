package org.cardanofoundation.rewards.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import rest.koios.client.backend.api.base.exception.ApiException;

public interface EpochStakeFetchingService {
  CompletableFuture<Boolean> fetchData(List<String> stakeAddressList) throws ApiException;

  List<String> getStakeAddressListNeedFetchData(List<String> stakeAddressList);
}
