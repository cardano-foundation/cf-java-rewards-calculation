package org.cardanofoundation.rewards.adapots;

import java.math.BigInteger;

import org.cardanofoundation.rewards.service.EpochParamService;
import org.cardanofoundation.rewards.service.impl.AdaPotsServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.cardanofoundation.rewards.common.entity.EpochParam;
import org.cardanofoundation.rewards.repository.AdaPotsRepository;
import org.cardanofoundation.rewards.repository.EpochRepository;

@SpringBootTest(classes = {AdaPotsRepository.class, EpochRepository.class})
@Disabled
public class AdaPotsTest {

  @Autowired AdaPotsRepository adaPotsRepository;

  @Autowired EpochRepository epochRepository;

  @Autowired
  AdaPotsServiceImpl adaPotsService;

  @MockBean
  EpochParamService epochParamService;

  @Test
  void Test_calculateTreasury() {
    int fromEpoch = 215;
    var adaPots = adaPotsRepository.getAdaPotsByEpochNoAfterOrderByEpochNo(fromEpoch);
    var epochFee = epochRepository.getEpochFeeByEpochGreater(fromEpoch);
    var epochParam =
        EpochParam.builder()
            .monetaryExpandRate(Double.valueOf(0.003))
            .treasuryGrowthRate(Double.valueOf(0.2))
            .decentralisation(Double.valueOf("0.76"))
            .build();
    for (int i = fromEpoch; i < 410; i++) {
      var lastAdaPots = adaPots.get(i - fromEpoch);
      var currentAdaPots = adaPots.get(i - fromEpoch + 1);
      var treasury =
          adaPotsService.calculateTreasury(
              epochParam,
              lastAdaPots.getReserves(),
              epochFee.get(i - fromEpoch).getFee(),
              lastAdaPots.getTreasury());
      System.out.println(
          "epoch: "
              + epochFee.get(i - fromEpoch).getEpochNo()
              + ", "
              + currentAdaPots.getEpochNo());
      Assertions.assertEquals(currentAdaPots.getTreasury(), treasury);
    }
  }

  @Test
  void Test_CalculateTreasuryEpoch215() {
    var epochParam =
        EpochParam.builder()
            .monetaryExpandRate(Double.valueOf(0.003))
            .treasuryGrowthRate(Double.valueOf(0.2))
            .decentralisation(Double.valueOf("0.78"))
            .build();
    var reserves = new BigInteger("13230232787944838");
    var fee = new BigInteger("7100593256");
    var lastTreasury = new BigInteger("48148335794725");
    var treasury = adaPotsService.calculateTreasury(epochParam, reserves, fee, lastTreasury);
    Assertions.assertEquals(new BigInteger("55876297807656"), treasury);
  }
}
