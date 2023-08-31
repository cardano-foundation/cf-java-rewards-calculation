package org.cardanofoundation.rewards.adapots;

import java.math.BigInteger;

import org.cardanofoundation.rewards.config.KoiosClient;
import org.cardanofoundation.rewards.repository.*;
import org.cardanofoundation.rewards.service.EpochParamService;
import org.cardanofoundation.rewards.service.RewardService;
import org.cardanofoundation.rewards.service.TxService;
import org.cardanofoundation.rewards.service.impl.AdaPotsServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.cardanofoundation.rewards.common.entity.EpochParam;

@Disabled
public class TreasuryTest {

  @Mock
  EpochRepository epochRepository;
  @Mock
  TxRepository txRepository;
  @Mock
  AddressTxBalanceRepository addressTxBalanceRepository;
  @Mock
  EpochParamService epochParamService;

  @Mock
  BlockRepository blockRepository;

  @Mock
  AdaPotsRepository adaPotsRepository;

  @Mock
  RewardService rewardService;

  @Mock
  TxService txService;

  @Mock
  KoiosClient koiosClient;
  AdaPotsServiceImpl adaPotsService;

  @BeforeEach
  void setup() {
    adaPotsService =
        new AdaPotsServiceImpl(
            epochRepository,
            txRepository,
            addressTxBalanceRepository,
            epochParamService,
            blockRepository,
            adaPotsRepository,
            rewardService,
            txService,
            koiosClient);
  }

  @Test
  void Test_CalculateTreasuryEpoch215() {
    var epochParam =
        EpochParam.builder()
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0.78"))
            .build();
    var reserves = new BigInteger("13230232787944840");
    var fee = new BigInteger("7100593256");
    var lastTreasury = new BigInteger("48148335794725");
    int totalBlockMinted = 4625;
    var treasury =
        adaPotsService.calculateTreasuryWithEta(
            epochParam, reserves, fee, lastTreasury, totalBlockMinted);
    Assertions.assertEquals(new BigInteger("55876297807656"), treasury);
  }

  @Test
  void Test_CalculateTreasuryEpoch400() {
    var epochParam =
        EpochParam.builder()
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0"))
            .build();
    var reserves = new BigInteger("9426301009576749");
    var fee = new BigInteger("100057069132");
    var lastTreasury = new BigInteger("1192381615518303");
    int totalBlockMinted = 20970;
    var treasury =
        adaPotsService.calculateTreasuryWithEta(
            epochParam, reserves, fee, lastTreasury, totalBlockMinted);
    Assertions.assertEquals(new BigInteger("1198058432944920"), treasury);
  }

  @Test
  void Test_CalculateTreasuryEpoch390() {
    var epochParam =
        EpochParam.builder()
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0"))
            .build();
    var reserves = new BigInteger("9605003170159443");
    var fee = new BigInteger("115998871359");
    var lastTreasury = new BigInteger("1136579323491238");
    int totalBlockMinted = 21220;
    var treasury =
        adaPotsService.calculateTreasuryWithEta(
            epochParam, reserves, fee, lastTreasury, totalBlockMinted);
    Assertions.assertEquals(new BigInteger("1142264335933469"), treasury);
  }
}
