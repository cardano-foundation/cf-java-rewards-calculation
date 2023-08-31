package org.cardanofoundation.rewards.epochstake;

import java.math.BigInteger;
import java.util.*;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.projection.DelegatorStakeUTXOProjection;
import org.cardanofoundation.rewards.service.PoolService;
import org.cardanofoundation.rewards.service.TxService;
import org.cardanofoundation.rewards.service.impl.EpochStakeServiceImpl;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.rewards.projection.DelegatorReceivedRewardProjection;
import org.cardanofoundation.rewards.projection.DelegatorWithdrawalProjection;
import org.cardanofoundation.rewards.repository.DelegationRepository;
import org.cardanofoundation.rewards.repository.EpochStakeRepository;
import org.cardanofoundation.rewards.repository.RewardRepository;
import org.cardanofoundation.rewards.repository.WithdrawalRepository;

@ExtendWith(MockitoExtension.class)
public class EpochStakeServiceTest {
  EpochStakeServiceImpl epochStakeService;

  @Mock DelegationRepository delegationRepository;

  @Mock EpochStakeRepository epochStakeRepository;

  @Mock
  TxService txService;

  @Mock
  PoolService poolService;

  @Mock WithdrawalRepository withdrawalRepository;

  @Mock RewardRepository rewardRepository;

  @BeforeEach
  void init() {
    epochStakeService =
        new EpochStakeServiceImpl(
            delegationRepository,
            epochStakeRepository,
            txService,
            poolService,
            withdrawalRepository,
            rewardRepository);
  }

  @Test
  void Test_calculateEpochStake() {
    List<DelegatorStakeUTXOProjection> delegatorStakeUTXOProjections = new ArrayList<>();
    delegatorStakeUTXOProjections.add(
        new DelegatorStakeUTXOProjection(1L, new BigInteger("100"), 1L));
    delegatorStakeUTXOProjections.add(
        new DelegatorStakeUTXOProjection(2L, new BigInteger("200"), 1L));
    delegatorStakeUTXOProjections.add(
        new DelegatorStakeUTXOProjection(3L, new BigInteger("300"), 2L));

    List<DelegatorWithdrawalProjection> delegatorWithdrawalProjections = new ArrayList<>();
    delegatorWithdrawalProjections.add(new DelegatorWithdrawalProjection(1L, new BigInteger("50")));
    delegatorWithdrawalProjections.add(new DelegatorWithdrawalProjection(3L, new BigInteger("20")));
    delegatorWithdrawalProjections.add(
        new DelegatorWithdrawalProjection(100L, new BigInteger("50")));

    List<DelegatorReceivedRewardProjection> delegatorReceivedRewardProjections = new ArrayList<>();
    delegatorReceivedRewardProjections.add(
        new DelegatorReceivedRewardProjection(1L, new BigInteger("200")));
    delegatorReceivedRewardProjections.add(
        new DelegatorReceivedRewardProjection(3L, new BigInteger("200")));

    Mockito.when(
            delegationRepository.findTotalStakedTilTx(
                Mockito.anyLong(), Mockito.anyInt(), Mockito.anySet()))
        .thenReturn(delegatorStakeUTXOProjections);
    Mockito.when(rewardRepository.findTotalReceivedRewardsTilEpochNo(Mockito.anyInt()))
        .thenReturn(delegatorReceivedRewardProjections);
    Mockito.when(withdrawalRepository.findTotalWithdrawalsTilTx(Mockito.anyLong()))
        .thenReturn(delegatorWithdrawalProjections);

    List<EpochStake> epochStakes =
        new ArrayList<>(epochStakeService.calculateEpochStakeOfEpoch(220).stream().toList());
    epochStakes.sort(Comparator.comparing(EpochStake::getStakeAddressId));
    Assertions.assertEquals(3, epochStakes.size());
    Assertions.assertEquals(1L, epochStakes.get(0).getStakeAddressId());
    Assertions.assertEquals(1L, epochStakes.get(0).getStakeAddressId());
    Assertions.assertEquals(new BigInteger("250"), epochStakes.get(0).getAmount());
    Assertions.assertEquals(2L, epochStakes.get(1).getStakeAddressId());
    Assertions.assertEquals(new BigInteger("200"), epochStakes.get(1).getAmount());
    Assertions.assertEquals(3L, epochStakes.get(2).getStakeAddressId());
    Assertions.assertEquals(new BigInteger("480"), epochStakes.get(2).getAmount());
  }
}
