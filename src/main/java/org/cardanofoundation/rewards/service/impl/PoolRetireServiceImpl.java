package org.cardanofoundation.rewards.service.impl;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.common.entity.RewardType;
import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.service.PoolRetireService;
import org.cardanofoundation.rewards.service.StakeAddressService;
import org.cardanofoundation.rewards.service.TxService;
import org.springframework.stereotype.Service;

import org.cardanofoundation.rewards.common.entity.PoolHash;
import org.cardanofoundation.rewards.common.entity.Reward;
import org.cardanofoundation.rewards.common.entity.StakeAddress;
import org.cardanofoundation.rewards.common.PoolRetiredReward;
import org.cardanofoundation.rewards.projection.PoolUpdateRewardProjection;
import org.cardanofoundation.rewards.repository.PoolRetireRepository;
import org.cardanofoundation.rewards.repository.PoolUpdateRepository;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class PoolRetireServiceImpl implements PoolRetireService {

  PoolUpdateRepository poolUpdateRepository;
  PoolRetireRepository poolRetireRepository;
  StakeAddressService stakeAddressService;
  TxService txService;

  @Override
  public PoolRetiredReward getRefundRewards(int epochNo) {
    var txId = txService.getTxIdLedgerSnapshotOfEpoch(epochNo);
    var poolRetireIds = poolRetireRepository.getPoolIdRetiredTilTxInEpoch(txId, epochNo);
    log.info("Epoch no {}, tx snapshot {}", epochNo, txId);
    // 1. find the certificate applied in the epoch
    var poolUpdates =
        poolUpdateRepository.findLastPoolsRegistrationActivatedInEpoch(poolRetireIds, txId);

    var rewardAddrs =
        poolUpdates.stream()
            .map(PoolUpdateRewardProjection::getRewardAddrId)
            .collect(Collectors.toSet());

    // 2. Filter reward account of the pool.
    var rewardAddrsRegistered =
        stakeAddressService.getStakeAddressRegisteredTilEpoch(txId, rewardAddrs, epochNo);

    // 2.1 If it is not registered then the refund of that pool will go to the treasury
    var poolDoNotHaveRegisteredRewardAddressAvailable =
        poolUpdates.stream()
            .filter(
                poolUpdateRewardProjection ->
                    !rewardAddrsRegistered.contains(poolUpdateRewardProjection.getRewardAddrId()))
            .count();
    var additionTreasury =
        BigInteger.valueOf(poolDoNotHaveRegisteredRewardAddressAvailable)
            .multiply(RewardConstants.REFUND_ADA);

    // 2.2 If it is registered then it will get refund back
    List<Reward> refunds =
        poolUpdates.stream()
            .filter(
                poolUpdateRewardProjection ->
                    rewardAddrsRegistered.contains(poolUpdateRewardProjection.getRewardAddrId()))
            .map(
                poolUpdate ->
                    Reward.builder() // TODO set refund ada with parameter of epoch param
                        .pool(PoolHash.builder().id(poolUpdate.getPoolHashId()).build())
                        .poolId(poolUpdate.getPoolHashId())
                        .addr(StakeAddress.builder().id(poolUpdate.getRewardAddrId()).build())
                        .stakeAddressId(poolUpdate.getRewardAddrId())
                        .amount(RewardConstants.REFUND_ADA)
                        .type(RewardType.REFUND)
                        .earnedEpoch(epochNo)
                        .spendableEpoch(epochNo)
                        .build())
            .collect(Collectors.toList());
    return PoolRetiredReward.builder()
        .refundRewards(refunds)
        .additionTreasury(additionTreasury)
        .build();
  }

  @Override
  public Set<Long> getPoolRetiredIdTilEpoch(int epoch) {
    var txId = txService.getTxIdLedgerSnapshotOfEpoch(epoch);
    var poolRetireIds = poolRetireRepository.getPoolIdRetiredTilEpoch(txId, epoch);
    log.info(
        "Epoch {} pool retire size {} txId {} epochNo {}",
        epoch,
        poolRetireIds.size(),
        txId,
        epoch);
    return poolRetireIds;
  }
}
