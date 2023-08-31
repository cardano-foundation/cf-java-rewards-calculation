package org.cardanofoundation.rewards.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Collection;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.exception.PreviousAdaPotsNotFoundException;
import org.cardanofoundation.rewards.exception.RewardWithdrawalException;
import org.cardanofoundation.rewards.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import rest.koios.client.backend.api.base.exception.ApiException;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.common.entity.Reward;
import org.cardanofoundation.rewards.common.EpochEndData;
import org.cardanofoundation.rewards.exception.RefundConflictException;
import org.cardanofoundation.rewards.exception.StakeBalanceException;
import org.cardanofoundation.rewards.repository.EpochRepository;
import org.cardanofoundation.rewards.repository.PoolHistoryRepository;
import org.cardanofoundation.rewards.repository.TxRepository;
import org.cardanofoundation.rewards.repository.jdbc.JDBCEpochStakeRepository;
import org.cardanofoundation.rewards.repository.jdbc.JDBCRewardRepository;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class EpochEndServiceImpl implements EpochEndService {

  final AdaPotsService adaPotsService;
  final RewardService rewardService;
  final EpochStakeService epochStakeService;
  final EpochRepository epochRepository;
  final PoolRetireService poolRetireService;
  final TxRepository txRepository;
  final HardForkService hardForkService;
  final JDBCRewardRepository jdbcRewardRepository;
  final JDBCEpochStakeRepository jdbcEpochStakeRepository;
  final PoolHistoryRepository poolHistoryRepository;

  @Value("${application.network-magic}")
  int networkMagic;

  @Override
  public EpochEndData getEpochEndData(int adaPotsEpoch) throws ApiException {
    // epoch has past (n)
    int epochHasPast = adaPotsEpoch - 1;

    try {
      var adaPots = adaPotsService.calculateAdaPots(adaPotsEpoch); // n+1

      // activate stake of epoch n + 2 is balance of stake address in last block of epoch n
      // Refund reward is paid when cardano network change to new epoch.
      var poolRetiredReward = poolRetireService.getRefundRewards(adaPotsEpoch);
      // Current deposit = total deposit has calculated - refund reward
      adaPotsService.updateRefundDeposit(adaPots, poolRetiredReward);
      log.info(
          "Earned epoch {} refund size {}",
          adaPots.getEpochNo(),
          poolRetiredReward.getRefundRewards().size());
      var epochStakes = epochStakeService.calculateEpochStakeOfEpoch(epochHasPast);
      // Calculate reward member, leader will be earned in adaPotsEpoch
      var fee = txRepository.getFeeOfEpoch(epochHasPast);
      var stakeRewards =
          rewardService.calculateReward(adaPots.getEpochNo(), adaPots.getReserves(), fee);

      stakeRewards = hardForkService.handleRewardIssueForEachEpoch(
          networkMagic, adaPots.getEpochNo() - 1, stakeRewards);

      var totalStakeReward =
          stakeRewards.stream().map(Reward::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
      log.info("Ada pot final {}", adaPots);
      var totalStake =
          epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
      log.info(
          "Epoch {}, stake size: {}, total stake: {}",
          adaPots.getEpochNo(),
          epochStakes.size(),
          totalStake);
      log.info(
          "Earned epoch {} reward leader and member size {} total stake reward {}",
          adaPots.getEpochNo() - 1,
          stakeRewards.size(),
          totalStakeReward);
      return EpochEndData.builder()
          .adaPots(adaPots)
          .epochStakes(epochStakes)
          .refunds(poolRetiredReward.getRefundRewards())
          .stakeRewards(stakeRewards)
          .build();
    } catch (StakeBalanceException
             | RewardWithdrawalException
             | RefundConflictException
             | PreviousAdaPotsNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (ApiException e) {
      throw e;
    }
    return new EpochEndData();
  }

  @Override
  @Transactional
  public void saveEpochEndData(EpochEndData epochEndData) {
    // collect reward
    Collection<Reward> rewards = epochEndData.getStakeRewards();
    rewards.addAll(epochEndData.getRefunds());

    if (!ObjectUtils.isEmpty(epochEndData.getAdaPots())) {
      adaPotsService.save(epochEndData.getAdaPots());
    }
    if (!ObjectUtils.isEmpty(rewards)) {
      jdbcRewardRepository.saveAll(rewards);
    }
    if (!ObjectUtils.isEmpty(epochEndData.getEpochStakes())) {
      jdbcEpochStakeRepository.saveAll(epochEndData.getEpochStakes());
    }
    var epochOpt = epochRepository.findEpochByNo(epochEndData.getAdaPots().getEpochNo() - 1);
    if (epochOpt.isPresent()) {
      var epoch = epochOpt.get();
      epoch.setRewardsDistributed(rewardService.getDistributedReward(epoch.getNo()));
      epochRepository.save(epoch);
    }
  }

  private Double calculateRos(BigInteger totalDistributeReward, BigInteger totalActivateStake) {
    return new BigDecimal(totalDistributeReward)
        .divide(new BigDecimal(totalActivateStake), 18, RoundingMode.HALF_EVEN)
        .multiply(new BigDecimal(RewardConstants.EPOCH_PER_YEAR))
        .multiply(new BigDecimal(100))
        .doubleValue();
  }
}
