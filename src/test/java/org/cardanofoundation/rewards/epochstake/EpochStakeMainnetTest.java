package org.cardanofoundation.rewards.epochstake;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.service.impl.EpochStakeServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;

import org.cardanofoundation.rewards.repository.jdbc.JDBCEpochStakeRepository;

@SpringBootTest
@Disabled
public class EpochStakeMainnetTest {

  @Autowired
  EpochStakeServiceImpl epochStakeService;

  @Autowired JDBCEpochStakeRepository epochStakeRepository;

  @Test
  void Test_getEpochStakeOfEpoch400() {
    var epochStake = epochStakeService.calculateEpochStakeOfEpoch(399);
    var totalStake =
        epochStake.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    System.out.println("epoch stake size: " + epochStake.size());
    assertEquals(new BigInteger("24723813844048216"), totalStake);
    assertEquals(1272453, epochStake.size());
  }

  @Test
  void Test_getEpochStakeOfEpoch210() {
    var epochStake = epochStakeService.calculateEpochStakeOfEpoch(209);
    var totalStake =
        epochStake.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    System.out.println("epoch stake size: " + epochStake.size());
    assertEquals(new BigInteger("6057875150904311"), totalStake);
    assertEquals(17305, epochStake.size());
  }

  @Test
  void Test_getEpochStakeOfEpoch230() {
    var epochStake = epochStakeService.calculateEpochStakeOfEpoch(229);
    var totalStake =
        epochStake.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    System.out.println("epoch stake size: " + epochStake.size());
    assertEquals(new BigInteger("19636382936670747"), totalStake);
    assertEquals(74936, epochStake.size());
  }

  @Test
  void Test_getEpochStakeOfEpoch214() {
    var epochStake = epochStakeService.calculateEpochStakeOfEpoch(213);
    var totalStake =
        epochStake.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    System.out.println("epoch stake size: " + epochStake.size());
    // epochStakeRepository.saveAll(epochStake);
    assertEquals(new BigInteger("13382718156097189"), totalStake);
    assertEquals(38780, epochStake.size());
  }

  @Test
  void Test_getEpochStakeOfEpoch212() {
    var epochStake = epochStakeService.calculateEpochStakeOfEpoch(211);
    var totalStake =
        epochStake.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    System.out.println("epoch stake size: " + epochStake.size());
    assertEquals(new BigInteger("12106602864871837"), totalStake);
    assertEquals(30628, epochStake.size());
  }

  @Test
  void Test_getEpochStakeOfEpoch213() {
    var epochStake = epochStakeService.calculateEpochStakeOfEpoch(212);
    var totalStake =
        epochStake.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    System.out.println("epoch stake size: " + epochStake.size());
    assertEquals(new BigInteger("12758829109784350"), totalStake);
    assertEquals(35360, epochStake.size());
  }
}
