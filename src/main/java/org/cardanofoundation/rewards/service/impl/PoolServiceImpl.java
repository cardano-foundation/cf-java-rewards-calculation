package org.cardanofoundation.rewards.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.projection.PoolUpdateProjection;
import org.cardanofoundation.rewards.repository.EpochParamRepository;
import org.cardanofoundation.rewards.repository.PoolRetireRepository;
import org.cardanofoundation.rewards.repository.PoolUpdateRepository;
import org.cardanofoundation.rewards.repository.SlotLeaderRepository;
import org.cardanofoundation.rewards.service.PoolService;
import org.cardanofoundation.rewards.service.TxService;
import org.springframework.stereotype.Service;

import org.cardanofoundation.rewards.projection.PoolMintBlockProjection;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class PoolServiceImpl implements PoolService {

  PoolRetireRepository poolRetireRepository;
  PoolUpdateRepository poolUpdateRepository;
  TxService txService;
  SlotLeaderRepository slotLeaderRepository;
  EpochParamRepository epochParamRepository;

  @Override
  public Set<Long> getPoolCanStakeFromEpoch(
      int epoch) { // epoch 'n' and try to calculate activate stake epoch 'n + 2'
    int epochSnapshot = epoch + 1;
    var txIdSnapshot = txService.getTxIdLedgerSnapshotOfEpoch(epochSnapshot);
    log.info("Pool can stake from epoch {}, tx id {}", epochSnapshot, txIdSnapshot);
    var txIdActivateStake = txService.getTxIdLedgerSnapshotOfEpoch(epoch);
    var mPoolsRegistered = poolUpdateRepository.findLastPoolsRegistrationIdByTxId(txIdSnapshot);
    var mLastPoolRetiredCertificate =
        poolRetireRepository.getLastPoolRetired(txIdActivateStake, epoch).stream()
            .collect(
                Collectors.toMap(
                    PoolUpdateProjection::getPoolHashId, PoolUpdateProjection::getTxId));
    return mPoolsRegistered.stream()
        .filter(
            certPoolUpdate -> {
              if (mLastPoolRetiredCertificate.containsKey(certPoolUpdate.getPoolHashId())) {
                return mLastPoolRetiredCertificate.get(certPoolUpdate.getPoolHashId())
                    < certPoolUpdate.getTxId();
              }
              return true;
            })
        .map(PoolUpdateProjection::getPoolHashId)
        .collect(Collectors.toSet());
  }

  @Override
  public Map<Long, BigDecimal> getPoolPerformanceInEpoch(
      int epochNo, List<EpochStake> epochStakes) {
    var epochDecentralisation =
        epochParamRepository.getEpochParamByEpochNo(epochNo).get().getDecentralisation();
    var poolMintBlocks = slotLeaderRepository.getPoolMintNumberBlockInEpoch(epochNo);
    if (epochDecentralisation >= RewardConstants.ADJUSTMENT_POOL_PERFORMANCE_DECENTRALISATION) {
      return poolMintBlocks.stream()
          .collect(
              Collectors.toMap(
                  PoolMintBlockProjection::getPoolId, poolMintBlockProjection -> BigDecimal.ONE));
    }
    var mPoolStake =
        epochStakes.stream()
            .collect(
                Collectors.groupingBy(
                    EpochStake::getPoolId,
                    Collectors.reducing(BigInteger.ZERO, EpochStake::getAmount, BigInteger::add)));
    var totalEpochStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var totalBlock = slotLeaderRepository.countBlockMintedByPoolInEpoch(epochNo);
    return poolMintBlocks.stream()
        .filter(poolMintBlockProjection -> Objects.nonNull(poolMintBlockProjection.getPoolId()))
        .collect(
            Collectors.toMap(
                PoolMintBlockProjection::getPoolId,
                poolMintBlock ->
                    getPoolPerformanceOfPool(
                        mPoolStake.get(poolMintBlock.getPoolId()),
                        totalEpochStake,
                        poolMintBlock.getTotalBlock().intValue(),
                        totalBlock)));
  }

  public Map<Long, BigDecimal> getEstimatedBlockOfPoolInEpoch(
      int epochNo, List<EpochStake> epochStakes) {
    var epochDecentralisation =
        epochParamRepository.getEpochParamByEpochNo(epochNo).get().getDecentralisation();
    var poolMintBlocks = slotLeaderRepository.getPoolMintNumberBlockInEpoch(epochNo);
    var mPoolStake =
        epochStakes.stream()
            .collect(
                Collectors.groupingBy(
                    EpochStake::getPoolId,
                    Collectors.reducing(BigInteger.ZERO, EpochStake::getAmount, BigInteger::add)));
    var totalEpochStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    return poolMintBlocks.stream()
        .filter(poolMintBlockProjection -> Objects.nonNull(poolMintBlockProjection.getPoolId()))
        .collect(
            Collectors.toMap(
                PoolMintBlockProjection::getPoolId,
                poolMintBlock ->
                    getEstimatedBlockOfPool(
                        mPoolStake.get(poolMintBlock.getPoolId()),
                        totalEpochStake,
                        new BigDecimal(epochDecentralisation))));
  }

  //    The rewards that are produced by this formula are now adjusted by pool performance:
  //    we multiply by β/σa, where β is the fraction of all blocks produced by the pool during the
  // epoch
  //    and σa is the stake delegated to the pool relative to the active stake
  //    (i.e. total stake that is correctly delegated to a non-retired pool).
  public BigDecimal getPoolPerformanceOfPool(
      BigInteger poolStake, BigInteger totalEpochStake, int blockHasMinted, int totalBlock) {
    BigDecimal beta =
        new BigDecimal(blockHasMinted).divide(new BigDecimal(totalBlock), 30, RoundingMode.DOWN);
    BigDecimal sigmaA =
        new BigDecimal(poolStake).divide(new BigDecimal(totalEpochStake), 30, RoundingMode.DOWN);
    return beta.divide(sigmaA, 30, RoundingMode.DOWN);
  }

  public BigDecimal getEstimatedBlockOfPool(
      BigInteger poolStake, BigInteger totalEpochStake, BigDecimal epochDecentralisation) {
    if (epochDecentralisation.compareTo(BigDecimal.ONE) >= 0) {
      return BigDecimal.ZERO;
    }
    return new BigDecimal(RewardConstants.EXPECTED_SLOT_PER_EPOCH)
        .multiply(BigDecimal.ONE.subtract(epochDecentralisation))
        .multiply(new BigDecimal(poolStake))
        .divide(new BigDecimal(totalEpochStake), 18, RoundingMode.HALF_EVEN);
  }
}
