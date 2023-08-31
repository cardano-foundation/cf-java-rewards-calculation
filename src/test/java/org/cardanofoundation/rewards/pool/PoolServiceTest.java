package org.cardanofoundation.rewards.pool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.projection.PoolUpdateProjection;
import org.cardanofoundation.rewards.repository.EpochParamRepository;
import org.cardanofoundation.rewards.repository.PoolRetireRepository;
import org.cardanofoundation.rewards.repository.PoolUpdateRepository;
import org.cardanofoundation.rewards.repository.SlotLeaderRepository;
import org.cardanofoundation.rewards.service.TxService;
import org.cardanofoundation.rewards.service.impl.PoolServiceImpl;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.rewards.common.entity.EpochParam;
import org.cardanofoundation.rewards.projection.PoolMintBlockProjection;

@ExtendWith(MockitoExtension.class)
public class PoolServiceTest {

  @Mock
  TxService txService;

  @Mock
  PoolRetireRepository poolRetireRepository;

  @Mock
  PoolUpdateRepository poolUpdateRepository;

  @Mock
  SlotLeaderRepository slotLeaderRepository;

  @Mock
  EpochParamRepository epochParamRepository;

  private PoolServiceImpl poolService;

  @BeforeEach
  void setup() {
    poolService =
        new PoolServiceImpl(
            poolRetireRepository,
            poolUpdateRepository,
            txService,
            slotLeaderRepository,
            epochParamRepository);
  }

  // epoch 215
  @Test
  void Test_estimateBlockWithDecentralizationSmallerThanZeroPointEight() {
    double d = Double.parseDouble("0.74");
    var poolStake = new BigInteger("239441181404825");
    var totalStake = new BigInteger("13864141914846088");
    BigDecimal blockEstimated =
        poolService.getEstimatedBlockOfPool(poolStake, totalStake, new BigDecimal(d));
    Assertions.assertEquals(
        Double.valueOf(" 96.99"), blockEstimated.setScale(2, RoundingMode.HALF_EVEN).doubleValue());
  }

  // epoch 220
  @Test
  void Test_getEstimatedBlockOfPoolInEpoch() {
    List<EpochStake> epochStakes = new ArrayList<>();
    epochStakes.add(
        EpochStake.builder().amount(new BigInteger("27523186000000")).poolId(1L).build());
    epochStakes.add(EpochStake.builder().amount(new BigInteger("299296")).poolId(1L).build());
    epochStakes.add(
        EpochStake.builder().amount(new BigInteger("110409318192212")).poolId(2L).build());
    epochStakes.add(
        EpochStake.builder().amount(new BigInteger("15722056138818018")).poolId(3L).build());

    List<PoolMintBlockProjection> poolMintBlockProjections = new ArrayList<>();
    poolMintBlockProjections.add(
        PoolMintBlockProjection.builder().poolId(1L).totalBlock(10L).build());
    poolMintBlockProjections.add(
        PoolMintBlockProjection.builder().poolId(2L).totalBlock(60L).build());

    Mockito.when(epochParamRepository.getEpochParamByEpochNo(220))
        .thenReturn(
            Optional.ofNullable(
                EpochParam.builder().decentralisation(Double.valueOf("0.64")).build()));
    Mockito.when(slotLeaderRepository.getPoolMintNumberBlockInEpoch(220))
        .thenReturn(poolMintBlockProjections);

    var mPoolBlocks = poolService.getEstimatedBlockOfPoolInEpoch(220, epochStakes);
    Assertions.assertEquals(2, mPoolBlocks.size());
    Assertions.assertEquals(
        Double.valueOf("13.49"),
        mPoolBlocks.get(1L).setScale(2, RoundingMode.HALF_EVEN).doubleValue());
    Assertions.assertEquals(
        Double.valueOf("54.13"),
        mPoolBlocks.get(2L).setScale(2, RoundingMode.HALF_EVEN).doubleValue());
  }

  // epoch 220
  @Test
  void Test_getPoolPerformanceInEpoch() {
    List<EpochStake> epochStakes = new ArrayList<>();
    epochStakes.add(
        EpochStake.builder().amount(new BigInteger("27523186000000")).poolId(1L).build());
    epochStakes.add(EpochStake.builder().amount(new BigInteger("299296")).poolId(1L).build());
    epochStakes.add(
        EpochStake.builder().amount(new BigInteger("110409318192212")).poolId(2L).build());
    epochStakes.add(
        EpochStake.builder().amount(new BigInteger("15722056138818018")).poolId(3L).build());

    List<PoolMintBlockProjection> poolMintBlockProjections = new ArrayList<>();
    poolMintBlockProjections.add(
        PoolMintBlockProjection.builder().poolId(1L).totalBlock(10L).build());
    poolMintBlockProjections.add(
        PoolMintBlockProjection.builder().poolId(2L).totalBlock(60L).build());

    Mockito.when(epochParamRepository.getEpochParamByEpochNo(220))
        .thenReturn(
            Optional.ofNullable(
                EpochParam.builder().decentralisation(Double.valueOf("0.64")).build()));
    Mockito.when(slotLeaderRepository.getPoolMintNumberBlockInEpoch(220))
        .thenReturn(poolMintBlockProjections);
    Mockito.when(slotLeaderRepository.countBlockMintedByPoolInEpoch(220)).thenReturn(7839);

    var mPoolPer = poolService.getPoolPerformanceInEpoch(220, epochStakes);
    Assertions.assertEquals(2, mPoolPer.size());
    Assertions.assertEquals(
        Double.valueOf("0.74"), mPoolPer.get(1L).setScale(2, RoundingMode.HALF_EVEN).doubleValue());
    Assertions.assertEquals(
        Double.valueOf("1.1"), mPoolPer.get(2L).setScale(2, RoundingMode.HALF_EVEN).doubleValue());
  }

  @Test
  void Test_getPoolCanStakeWithRegisGreater() {
    PoolUpdateProjection pRegis1 = Mockito.mock(PoolUpdateProjection.class);
    Mockito.when(pRegis1.getTxId()).thenReturn(1L);
    Mockito.when(pRegis1.getPoolHashId()).thenReturn(1L);

    PoolUpdateProjection pRegis2 = Mockito.mock(PoolUpdateProjection.class);
    Mockito.when(pRegis2.getTxId()).thenReturn(3L);
    Mockito.when(pRegis2.getPoolHashId()).thenReturn(1L);

    PoolUpdateProjection pDeRegis = Mockito.mock(PoolUpdateProjection.class);
    Mockito.when(pDeRegis.getTxId()).thenReturn(2L);
    Mockito.when(pDeRegis.getPoolHashId()).thenReturn(1L);

    List<PoolUpdateProjection> pRes = new ArrayList<>(List.of(pRegis1, pRegis2));
    List<PoolUpdateProjection> pDeRes = new ArrayList<>(List.of(pDeRegis));

    Mockito.when(poolUpdateRepository.findLastPoolsRegistrationIdByTxId(Mockito.anyLong()))
        .thenReturn(pRes);
    Mockito.when(poolRetireRepository.getLastPoolRetired(Mockito.anyLong(), Mockito.anyInt()))
        .thenReturn(pDeRes);
    Set<Long> poolIds = poolService.getPoolCanStakeFromEpoch(220);
    Assertions.assertTrue(poolIds.contains(1L));
  }

  @Test
  void Test_getPoolCanStakeWithRegisSmaller() {
    PoolUpdateProjection pRegis1 = Mockito.mock(PoolUpdateProjection.class);
    Mockito.when(pRegis1.getTxId()).thenReturn(1L);
    Mockito.when(pRegis1.getPoolHashId()).thenReturn(1L);

    PoolUpdateProjection pRegis2 = Mockito.mock(PoolUpdateProjection.class);
    Mockito.when(pRegis2.getTxId()).thenReturn(3L);
    Mockito.when(pRegis2.getPoolHashId()).thenReturn(1L);

    PoolUpdateProjection pDeRegis = Mockito.mock(PoolUpdateProjection.class);
    Mockito.when(pDeRegis.getTxId()).thenReturn(5L);
    Mockito.when(pDeRegis.getPoolHashId()).thenReturn(1L);

    List<PoolUpdateProjection> pRes = new ArrayList<>(List.of(pRegis1, pRegis2));
    List<PoolUpdateProjection> pDeRes = new ArrayList<>(List.of(pDeRegis));

    Mockito.when(poolUpdateRepository.findLastPoolsRegistrationIdByTxId(Mockito.anyLong()))
        .thenReturn(pRes);
    Mockito.when(poolRetireRepository.getLastPoolRetired(Mockito.anyLong(), Mockito.anyInt()))
        .thenReturn(pDeRes);
    Set<Long> poolIds = poolService.getPoolCanStakeFromEpoch(220);
    Assertions.assertTrue(poolIds.isEmpty());
  }
}
