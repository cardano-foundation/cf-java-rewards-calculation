package org.cardanofoundation.rewards.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.common.entity.*;
import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.service.*;
import org.springframework.stereotype.Service;

import org.cardanofoundation.rewards.projection.PoolConfigProjection;
import org.cardanofoundation.rewards.repository.PoolOwnerRepository;
import org.cardanofoundation.rewards.repository.RewardRepository;
import org.cardanofoundation.rewards.repository.SlotLeaderRepository;
import org.cardanofoundation.rewards.repository.WithdrawalRepository;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class RewardServiceImpl implements RewardService {

  EpochParamService epochParamService;
  RewardRepository rewardRepository;
  WithdrawalRepository withdrawalRepository;
  PoolUpdateService poolUpdateService;
  EpochStakeService epochStakeService;
  PoolOwnerRepository poolOwnerRepository;
  TxService txService;
  StakeAddressService stakeAddressService;
  PoolService poolService;
  SlotLeaderRepository slotLeaderRepository;

  @Override
  public BigInteger getTotalRemainRewardOfEpoch(int epoch) {
    var totalReward = getNonNullAmount(rewardRepository.getTotalRewardToEpoch(epoch));
    log.info("Total reward to the end of epoch {} is {}", epoch, totalReward);
    var totalWithdrawal =
        getNonNullAmount(withdrawalRepository.getTotalWithdrawalToEndEpoch(epoch - 1));
    log.info("Total withdrawal to the end of epoch {} is {}", epoch, totalWithdrawal);
    var remainReward = totalReward.subtract(totalWithdrawal);
    log.info("Remain reward to epoch {} is {}", epoch, remainReward);
    return remainReward;
  }

  private BigInteger getNonNullAmount(BigInteger amount) {
    if (Objects.isNull(amount)) {
      amount = BigInteger.ZERO;
    }
    return amount;
  }

  @Override
  public BigInteger getTotalRewardOfEpochByType(int epoch, RewardType rewardType) {
    return getNonNullAmount(rewardRepository.getTotalRewardOfEpochByType(epoch, rewardType));
  }

  @Override
  public BigInteger getDistributedReward(int epoch) {
    return getNonNullAmount(rewardRepository.getTotalSpendableRewardExcludeRefund(epoch));
  }

  @Override
  public Collection<Reward> calculateReward(int epoch, BigInteger reserves, BigInteger fee) {
    var epochReward = epoch - 1;
    var epochParamOpt = epochParamService.getEpochParam(epochReward);
    if (epochParamOpt.isEmpty()) {
      log.warn("Found no epoch param with epoch no: {}", epochReward);
      return Collections.emptyList();
    }
    var epochParam = epochParamOpt.get();
    // this stake address map use to later filter which stake address is pool owner's, which
    // stake address is member's

    // for each active pool of this epoch, calculate and insert rewards
    var poolConfigs = poolUpdateService.findPoolHasMintedBlockInEpoch(epochReward);
    log.info("Reward pool size minted block {}", poolConfigs.size());
    if (poolConfigs.isEmpty()) {
      return new ArrayList<>();
    }

    var poolUpdateIds =
        poolConfigs.stream().map(PoolConfigProjection::getPoolUpdateId).collect(Collectors.toSet());
    var poolOwners = poolOwnerRepository.findAllPoolOwnerByPoolUpdateIds(poolUpdateIds);
    Map<Long, Set<Long>> mPoolUpdateIdAndPoolOwner = new ConcurrentHashMap<>();
    poolOwners.forEach(
        poolOwner -> {
          var puId = poolOwner.getPoolUpdateId();
          if (mPoolUpdateIdAndPoolOwner.containsKey(puId)) {
            mPoolUpdateIdAndPoolOwner.get(puId).add(poolOwner.getStakeAddressId());
          } else {
            mPoolUpdateIdAndPoolOwner.put(
                puId, new HashSet<>(Collections.singletonList(poolOwner.getStakeAddressId())));
          }
        });

    // get all activate stake of epoch will get reward
    var stakeBalances = epochStakeService.getAllEpochStakeByEpochNo(epochReward);
    var txIdSnapshot = txService.getTxIdLedgerSnapshotOfEpoch(epoch + 1);
    var totalBlockMinted = slotLeaderRepository.countBlockMintedByPoolInEpoch(epochReward);
    var sStakeAddressIds =
        stakeBalances.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    log.info("Reward epoch {} stake size {}", epochReward, stakeBalances.size());
    Collection<Reward> stakeRewards = new ConcurrentLinkedQueue<>();

    var leaderAddresses =
        poolConfigs.stream()
            .map(PoolConfigProjection::getRewardAddressId)
            .collect(Collectors.toSet());
    sStakeAddressIds.addAll(leaderAddresses);

    var stakeAddressesAreRegistered =
        stakeAddressService.getStakeAddressRegisteredTilEpoch(
            txIdSnapshot, sStakeAddressIds, epoch + 1);
    Map<Long, BigDecimal> poolsPerformance =
        poolService.getPoolPerformanceInEpoch(epochReward, stakeBalances);

    Map<Long, List<EpochStake>> mPoolIdAndEpochStakes =
        stakeBalances.stream().collect(Collectors.groupingBy(EpochStake::getPoolId));

    poolConfigs.parallelStream()
        .forEach(
            poolConfig ->
                stakeRewards.addAll(
                    calculateRewardsOfAPool(
                        poolConfig,
                        reserves,
                        fee,
                        epochParam,
                        mPoolIdAndEpochStakes.get(poolConfig.getPoolId()),
                        epochReward,
                        mPoolUpdateIdAndPoolOwner.get(poolConfig.getPoolUpdateId()),
                        stakeAddressesAreRegistered,
                        poolsPerformance.get(poolConfig.getPoolId()),
                        totalBlockMinted)));

    epochStakeService.removeCacheEpochStakeByEpochNo(epochReward);
    return stakeRewards;
  }

  public Collection<Reward> calculateRewardsOfAPool(
      PoolConfigProjection poolConfigProjection,
      BigInteger reserves,
      BigInteger fee,
      EpochParam epochParam,
      List<EpochStake> epochStakeOfPool,
      int epochNo,
      Set<Long> poolOwnerAddrIds,
      Set<Long> stakeAddressIdsAreRegistered,
      BigDecimal poolPerformance,
      int totalBlockMinted) {
    var totalStakeOfPool = calculateTotalStakeOfPool(epochStakeOfPool);
    var poolOwnerPledge = new BigDecimal(poolConfigProjection.getPledge());
    var totalAda = new BigDecimal(RewardConstants.TOTAL_ADA).subtract(new BigDecimal(reserves));

    var totalStakeOfPoolOwner =
        epochStakeOfPool.stream()
            .filter(epochStake -> poolOwnerAddrIds.contains(epochStake.getStakeAddressId()))
            .map(EpochStake::getAmount)
            .reduce(BigInteger.ZERO, BigInteger::add);
    // sigma in the formula
    var relativeStakeOfPool = totalStakeOfPool.divide(totalAda, 30, RoundingMode.DOWN);
    // s in the formula
    var relativePledgeStakeOfPoolOwner = poolOwnerPledge.divide(totalAda, 30, RoundingMode.DOWN);

    List<Reward> stakeRewards = new ArrayList<>();

    // If pool owner stake not enough with pledge so the pool will get no reward
    if (totalStakeOfPoolOwner.compareTo(poolConfigProjection.getPledge()) < 0) {
      log.warn(
          "Pool {} will not get reward because owner stake {} is smaller than pledge {}",
          poolConfigProjection.getPoolId(),
          totalStakeOfPoolOwner,
          poolConfigProjection.getPledge());
      // leader will get zero reward
      stakeRewards.add(Reward.builder()
          .type(RewardType.LEADER)
          .earnedEpoch(epochNo)
          .spendableEpoch(epochNo + 2)
          .addr(StakeAddress.builder().id(poolConfigProjection.getRewardAddressId()).build())
          .stakeAddressId(poolConfigProjection.getRewardAddressId())
          .pool(PoolHash.builder().id(poolConfigProjection.getPoolId()).build())
          .poolId(poolConfigProjection.getPoolId())
          .amount(BigInteger.ZERO)
          .build());
      return stakeRewards;
    }

    var relativeStakeOfPoolOwner =
        new BigDecimal(totalStakeOfPoolOwner).divide(totalAda, 30, RoundingMode.DOWN);

    BigDecimal poolReward =
        calculatePoolReward(
            epochParam,
            reserves,
            fee,
            relativeStakeOfPool,
            relativePledgeStakeOfPoolOwner,
            poolPerformance,
            totalBlockMinted);

    // For each stake address, calculate reward
    epochStakeOfPool.forEach(
        stakeBalance -> {
          // this stake address is a member's stake address, calculate pool member
          if (!poolOwnerAddrIds.contains(stakeBalance.getStakeAddressId())
              && !stakeBalance
              .getStakeAddressId()
              .equals(poolConfigProjection.getRewardAddressId())) {
            // reward
            var stakeBalanceOfMember = new BigDecimal(stakeBalance.getAmount());
            // t in the formula
            var relativeStakeOfMember =
                stakeBalanceOfMember.divide(totalAda, 30, RoundingMode.DOWN);

            RewardParam param =
                RewardParam.builder()
                    .stakeAddressId(stakeBalance.getStakeAddressId())
                    .relativeStake(relativeStakeOfMember)
                    .poolReward(poolReward.setScale(0, RoundingMode.DOWN))
                    .relativeStakeOfPool(relativeStakeOfPool)
                    .poolConfigProjection(poolConfigProjection)
                    .epochNo(epochNo)
                    .build();

            Reward memberReward = calculateMemberReward(param);
            if (memberReward.getAmount().compareTo(BigInteger.ZERO) > 0) {
              if (stakeAddressIdsAreRegistered.contains(stakeBalance.getStakeAddressId())) {
                stakeRewards.add(memberReward);
              } else {
                log.debug(
                    "Stake {} of pool {} wouldn't earn member reward with amount {}",
                    memberReward.getStakeAddressId(),
                    memberReward.getPoolId(),
                    memberReward.getAmount());
              }
            }
          }
        });
    RewardParam param =
        RewardParam.builder()
            .stakeAddressId(poolConfigProjection.getRewardAddressId())
            .relativeStake(relativeStakeOfPoolOwner)
            .poolReward(poolReward.setScale(0, RoundingMode.DOWN))
            .relativeStakeOfPool(relativeStakeOfPool)
            .poolConfigProjection(poolConfigProjection)
            .epochNo(epochNo)
            .build();

    Reward poolOwnerReward = calculatePoolOwnerReward(param);
    // Leader reward can earn if reward address stake registration is activated
    if (stakeAddressIdsAreRegistered.contains(poolConfigProjection.getRewardAddressId())) {
      stakeRewards.add(poolOwnerReward);
    } else {
      log.debug(
          "Pool {} in epoch {} wouldn't earn leader reward because reward address {} didn't have registered yet amount {}",
          poolConfigProjection.getPoolId(),
          epochNo + 2,
          poolConfigProjection.getRewardAddressId(),
          poolOwnerReward.getAmount());
    }
    return stakeRewards;
  }

  private BigDecimal calculateTotalStakeOfPool(List<EpochStake> stakeBalances) {
    var totalStake =
        stakeBalances.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    return new BigDecimal(totalStake);
  }

  public BigDecimal calculatePoolReward(
      EpochParam epochParam,
      BigInteger reserves,
      BigInteger fee,
      BigDecimal relativeStakeOfPool,
      BigDecimal relativeStakeOfPoolOwner,
      BigDecimal poolPerformance,
      int totalBlockMinted) {

    // R in the formula
    var totalAvailableRewardOfEpoch =
        calculateTotalAvailableReward(reserves, fee, epochParam, totalBlockMinted)
            .setScale(0, RoundingMode.DOWN);
    var a0 = new BigDecimal(epochParam.getInfluence().toString());
    var z0 =
        BigDecimal.ONE.divide(
            new BigDecimal(epochParam.getOptimalPoolCount()), 30, RoundingMode.DOWN);

    // sigma' in the formula
    var min1 = z0.min(relativeStakeOfPool);
    // s' in the formula
    var min2 = z0.min(relativeStakeOfPoolOwner);
    MathContext mc = new MathContext(30, RoundingMode.DOWN);
    return (totalAvailableRewardOfEpoch.divide(BigDecimal.ONE.add(a0), 30, RoundingMode.DOWN))
        .multiply(
            min1.add(
                min2.multiply(a0, mc)
                    .multiply(
                        min1.subtract(
                                min2.multiply(
                                    z0.subtract(min1).divide(z0, 30, RoundingMode.DOWN), mc))
                            .divide(z0, 30, RoundingMode.DOWN),
                        mc)),
            mc)
        .multiply(poolPerformance, mc);
  }

  // calculate R in the formula
  public BigDecimal calculateTotalAvailableReward(
      BigInteger reserves, BigInteger fee, EpochParam epochParam, int totalBlockMinted) {
    var treasuryRate = new BigDecimal(epochParam.getTreasuryGrowthRate().toString());
    var expansionRate = new BigDecimal(epochParam.getMonetaryExpandRate().toString());
    BigDecimal eta;
    MathContext mc = new MathContext(18, RoundingMode.DOWN);
    if (epochParam.getDecentralisation() >= Double.valueOf("0.8")) {
      eta = BigDecimal.ONE;
    } else {
      BigDecimal slotPerEpoch = new BigDecimal(RewardConstants.EXPECTED_SLOT_PER_EPOCH);
      eta =
          BigDecimal.ONE.min(
              new BigDecimal(totalBlockMinted)
                  .divide(
                      BigDecimal.ONE
                          .subtract(new BigDecimal(epochParam.getDecentralisation().toString()))
                          .multiply(slotPerEpoch, mc),
                      18,
                      RoundingMode.DOWN));
    }

    var rewardPot =
        new BigDecimal(reserves)
            .multiply(expansionRate, mc)
            .multiply(eta, mc)
            .add(new BigDecimal(fee));
    return rewardPot.multiply(BigDecimal.ONE.subtract(treasuryRate), mc);
  }

  public Reward calculatePoolOwnerReward(RewardParam param) {
    BigDecimal rewardAmount;

    // m in the formula
    var poolMargin = new BigDecimal(param.getPoolConfigProjection().getMargin().toString());
    // c in the formula
    var poolFixedCost = new BigDecimal(param.getPoolConfigProjection().getFixedCost());
    // if pool reward less or equal to pool fixed cost -> pool owner's reward = poolReward
    MathContext mc = new MathContext(30, RoundingMode.DOWN);
    if (param.getPoolReward().compareTo(poolFixedCost) <= 0) {
      rewardAmount = param.getPoolReward();
    } else { // else apply the formula
      rewardAmount =
          poolFixedCost.add(
              param
                  .getPoolReward()
                  .subtract(poolFixedCost)
                  .multiply(
                      poolMargin.add(
                          BigDecimal.ONE
                              .subtract(poolMargin)
                              .multiply(param.getRelativeStake())
                              .divide(param.getRelativeStakeOfPool(), 30, RoundingMode.DOWN)),
                      mc));
    }
    return Reward.builder()
        .type(RewardType.LEADER)
        .earnedEpoch(param.getEpochNo())
        .spendableEpoch(param.getEpochNo() + 2)
        .addr(StakeAddress.builder().id(param.getStakeAddressId()).build())
        .stakeAddressId(param.getStakeAddressId())
        .pool(PoolHash.builder().id(param.getPoolConfigProjection().getPoolId()).build())
        .poolId(param.getPoolConfigProjection().getPoolId())
        .amount(rewardAmount.toBigInteger())
        .build();
  }

  public Reward calculateMemberReward(RewardParam param) {
    BigDecimal rewardAmount;
    // m in the formula
    var poolMargin = new BigDecimal(param.getPoolConfigProjection().getMargin().toString());
    // c in the formula
    var poolFixedCost = new BigDecimal(param.getPoolConfigProjection().getFixedCost());
    // if pool reward less or equal to pool fixed cost -> member's reward = 0
    if (param.getPoolReward().compareTo(poolFixedCost) <= 0) {
      rewardAmount = BigDecimal.ZERO;
    } else { // else apply the formula
      rewardAmount =
          param
              .getPoolReward()
              .subtract(poolFixedCost)
              .multiply(
                  BigDecimal.ONE
                      .subtract(poolMargin)
                      .multiply(param.getRelativeStake())
                      .divide(param.getRelativeStakeOfPool(), 30, RoundingMode.DOWN));
    }
    return Reward.builder()
        .type(RewardType.MEMBER)
        .earnedEpoch(param.getEpochNo())
        .spendableEpoch(param.getEpochNo() + 2)
        .stakeAddressId(param.getStakeAddressId())
        .addr(StakeAddress.builder().id(param.getStakeAddressId()).build())
        .pool(PoolHash.builder().id(param.getPoolConfigProjection().getPoolId()).build())
        .poolId(param.getPoolConfigProjection().getPoolId())
        .amount(rewardAmount.toBigInteger())
        .build();
  }
}
