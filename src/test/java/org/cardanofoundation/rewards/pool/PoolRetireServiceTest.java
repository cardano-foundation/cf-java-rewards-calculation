package org.cardanofoundation.rewards.pool;

import org.cardanofoundation.rewards.service.StakeAddressService;
import org.cardanofoundation.rewards.service.TxService;
import org.cardanofoundation.rewards.service.impl.PoolRetireServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.rewards.repository.PoolRetireRepository;
import org.cardanofoundation.rewards.repository.PoolUpdateRepository;

@ExtendWith(MockitoExtension.class)
public class PoolRetireServiceTest {

  PoolRetireServiceImpl poolRetireService;

  @Mock PoolUpdateRepository poolUpdateRepository;

  @Mock PoolRetireRepository poolRetireRepository;

  @Mock
  StakeAddressService stakeAddressService;

  @Mock
  TxService txService;

  @BeforeEach
  void init() {
    poolRetireService =
        new PoolRetireServiceImpl(
            poolUpdateRepository, poolRetireRepository, stakeAddressService, txService);
  }
}
