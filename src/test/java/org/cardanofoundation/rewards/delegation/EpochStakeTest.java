package org.cardanofoundation.rewards.delegation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.service.EpochStakeService;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.cardanofoundation.rewards.repository.DelegationRepository;

@SpringBootTest
@Disabled
public class EpochStakeTest {

  @Autowired DelegationRepository delegationRepository;

  @Autowired
  EpochStakeService epochStakeService;

  @Test
  void Test_getEpochStake() {
    List<Pair<Integer, BigInteger>> epochStakes = new ArrayList<>();
    epochStakes.add(Pair.of(209, new BigInteger("6057875150904311")));
    epochStakes.add(Pair.of(210, new BigInteger("10177811974822904")));
    epochStakes.add(Pair.of(211, new BigInteger("12106602864871837")));
    epochStakes.add(Pair.of(212, new BigInteger("12758829109784350")));
    for (Pair<Integer, BigInteger> epochStake : epochStakes) {
      var ess = epochStakeService.calculateEpochStakeOfEpoch(epochStake.getFirst());
      System.out.println(ess.size());
      var totalStake =
          ess.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
      System.out.println("Total stake: " + totalStake);
      Assertions.assertEquals((epochStake.getSecond()), totalStake);
    }
  }
}
