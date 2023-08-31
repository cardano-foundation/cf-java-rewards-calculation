package org.cardanofoundation.rewards.service.impl;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.exception.RewardWithdrawalException;
import org.cardanofoundation.rewards.exception.StakeBalanceException;
import org.cardanofoundation.rewards.projection.DelegatorReceivedRewardProjection;
import org.cardanofoundation.rewards.projection.DelegatorWithdrawalProjection;
import org.cardanofoundation.rewards.service.PoolService;
import org.cardanofoundation.rewards.service.TxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.common.entity.PoolHash;
import org.cardanofoundation.rewards.common.entity.StakeAddress;
import org.cardanofoundation.rewards.repository.DelegationRepository;
import org.cardanofoundation.rewards.repository.EpochStakeRepository;
import org.cardanofoundation.rewards.repository.RewardRepository;
import org.cardanofoundation.rewards.repository.WithdrawalRepository;
import org.cardanofoundation.rewards.service.EpochStakeService;

@Service
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class EpochStakeServiceImpl implements EpochStakeService {

  DelegationRepository delegationRepository;
  EpochStakeRepository epochStakeRepository;
  TxService txService;

  WithdrawalRepository withdrawalRepository;

  RewardRepository rewardRepository;

  HashMap<Integer, List<EpochStake>> cacheEpochStake;

  PoolService poolService;

  public EpochStakeServiceImpl(
      DelegationRepository delegationRepository,
      EpochStakeRepository epochStakeRepository,
      TxService txService,
      PoolService poolService,
      WithdrawalRepository withdrawalRepository,
      RewardRepository rewardRepository) {
    this.delegationRepository = delegationRepository;
    this.epochStakeRepository = epochStakeRepository;
    this.txService = txService;
    this.poolService = poolService;
    this.withdrawalRepository = withdrawalRepository;
    this.rewardRepository = rewardRepository;
    this.cacheEpochStake = new HashMap<>();
  }

  @Override
  @Transactional(readOnly = true)
  public Collection<EpochStake> calculateEpochStakeOfEpoch(int epochStake) {
    var lastTxId = txService.getTxIdLedgerSnapshotOfEpoch(epochStake);
    log.info(
        "Epoch stake from Tx snapshot {} epoch snapshot {} epoch activate stake {}",
        lastTxId,
        epochStake - 1,
        epochStake + 1);
    var poolActivateIds = poolService.getPoolCanStakeFromEpoch(epochStake - 1);
    var totalStakes =
        delegationRepository.findTotalStakedTilTx(lastTxId, epochStake + 1, poolActivateIds);
    var totalReceivedRewards = rewardRepository.findTotalReceivedRewardsTilEpochNo(epochStake - 1);
    var totalWithdrawals = withdrawalRepository.findTotalWithdrawalsTilTx(lastTxId);

    Map<Long, BigInteger> totalReceivedRewardsMap =
        totalReceivedRewards.stream()
            .collect(
                Collectors.toMap(
                    DelegatorReceivedRewardProjection::getStakeAddressId,
                    DelegatorReceivedRewardProjection::getTotalReceivedReward));

    Map<Long, BigInteger> totalWithdrawalsMap =
        totalWithdrawals.stream()
            .collect(
                Collectors.toMap(
                    DelegatorWithdrawalProjection::getStakeAddressId,
                    DelegatorWithdrawalProjection::getTotalWithdrawal));
    List<EpochStake> epochStakes =
        totalStakes.stream()
            .map(
                stakeBalance -> {
                  var stakeAddressId = stakeBalance.getStakeAddressId();
                  var totalStaked = stakeBalance.getTotalUXTOStake();
                  if (Objects.isNull(totalStaked)) {
                    totalStaked = BigInteger.ZERO;
                  }
                  var poolId = stakeBalance.getPoolId();

                  // get total received reward for current stake address id
                  var receivedReward = totalReceivedRewardsMap.get(stakeAddressId);
                  if (Objects.isNull(receivedReward)) {
                    receivedReward = BigInteger.ZERO;
                  }

                  // get total withdrawal for current stake address id
                  var totalWithdrawal = totalWithdrawalsMap.get(stakeAddressId);
                  if (Objects.isNull(totalWithdrawal)) {
                    totalWithdrawal = BigInteger.ZERO;
                  }

                  var balance = totalStaked.add(receivedReward).subtract(totalWithdrawal);
                  if (totalWithdrawal.compareTo(receivedReward) > 0) {
                    log.warn(
                        "Stake key {} received reward {}, total withdrawal {}",
                        stakeAddressId,
                        receivedReward,
                        totalWithdrawal);
                    throw new RewardWithdrawalException(
                        String.format(
                            "Withdrawal is %s greater than received reward %s",
                            totalWithdrawal, receivedReward));
                  }
                  if (balance.compareTo(BigInteger.ZERO) < 0) {
                    log.warn(
                        "Stake key {}, utxo balance {}, received reward {}, total withdrawal {}",
                        stakeAddressId,
                        totalStaked,
                        receivedReward,
                        totalWithdrawal);
                    throw new StakeBalanceException("Stake balance must be greater or equal 0");
                  }
                  return EpochStake.builder()
                      .epochNo(epochStake + 1)
                      .addr(StakeAddress.builder().id(stakeAddressId).build())
                      .stakeAddressId(stakeAddressId)
                      .poolId(poolId)
                      .pool(PoolHash.builder().id(poolId).build())
                      .amount(balance)
                      .build();
                })
            .collect(Collectors.toList());
    cacheEpochStake.put(epochStake + 1, epochStakes);
    //
    return epochStakes;
  }

  @Override
  public List<EpochStake> getAllEpochStakeByEpochNo(int epoch) {
    if (cacheEpochStake.containsKey(epoch)) {
      return cacheEpochStake.get(epoch);
    }
    return epochStakeRepository.findEpochStakeByEpochNo(epoch);
  }

  @Override
  public void removeCacheEpochStakeByEpochNo(int epoch) {
    List<Integer> epochWillBeDeleted = new ArrayList<>();
    for (Integer epochKey : cacheEpochStake.keySet()) {
      if (epochKey <= epoch) {
        epochWillBeDeleted.add(epochKey);
      }
    }
    for (Integer epochKey : epochWillBeDeleted) {
      cacheEpochStake.remove(epochKey);
    }
  }
}
