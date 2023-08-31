package org.cardanofoundation.rewards.epoch;

import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.service.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rest.koios.client.backend.api.base.exception.ApiException;
import org.junit.jupiter.api.Test;

@SpringBootTest
@Slf4j
@Disabled
public class EpochEndTest {

  @Autowired
  EpochEndService epochEndService;

  @Test
  void Test_CalculateAdaPot() throws ApiException {
    // epoch has past (n)
    var adaPotsEpoch = 213;
    epochEndService.getEpochEndData(adaPotsEpoch);
  }
}
