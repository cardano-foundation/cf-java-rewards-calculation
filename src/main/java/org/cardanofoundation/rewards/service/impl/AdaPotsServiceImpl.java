package org.cardanofoundation.rewards.service.impl;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.common.entity.*;
import org.cardanofoundation.rewards.common.enumeration.EraType;

import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.exception.PreviousAdaPotsNotFoundException;
import org.cardanofoundation.rewards.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import rest.koios.client.backend.api.base.exception.ApiException;
import org.cardanofoundation.rewards.common.PoolRetiredReward;
import org.cardanofoundation.rewards.config.KoiosClient;
import org.cardanofoundation.rewards.exception.RefundConflictException;
import org.cardanofoundation.rewards.service.AdaPotsService;
import org.cardanofoundation.rewards.service.EpochParamService;
import org.cardanofoundation.rewards.service.RewardService;
import org.cardanofoundation.rewards.service.TxService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Service
public class AdaPotsServiceImpl implements AdaPotsService {

  final EpochRepository epochRepository;
  final TxRepository txRepository;
  final AddressTxBalanceRepository addressTxBalanceRepository;
  final EpochParamService epochParamService;
  final BlockRepository blockRepository;
  final AdaPotsRepository adaPotsRepository;
  final RewardService rewardService;
  final TxService txService;
  final KoiosClient koiosClient;

  @Override
  public AdaPots calculateAdaPots(int epochNo) throws PreviousAdaPotsNotFoundException {
    log.warn("Calculate ada pots for epoch: {}", epochNo);
    var lastEpochNo = epochNo - 1;
    var lastEpochOptional = epochRepository.findEpochByNo(lastEpochNo);
    var currentEpochOptional = epochRepository.findEpochByNo(epochNo);
    if (lastEpochOptional.isPresent() && currentEpochOptional.isPresent()) {
      var epochParam =
          epochParamService
              .getEpochParam(lastEpochNo)
              .orElse(EpochParam.builder().treasuryGrowthRate(0d).monetaryExpandRate(0d).build());
      long startTime = System.currentTimeMillis();

      // Ada pots of epoch n is the ledger snapshot of first block of epoch n
      var block = blockRepository.getFirstBlockByEpochNo(epochNo);
      Epoch lastEpoch = lastEpochOptional.get();
      var txId = txService.getTxIdAdaPotsOfEpoch(epochNo);
      BigInteger totalAdaSupplyOnChain =
          addressTxBalanceRepository.getTotalSupplyOfLovelaceFromBeginToTxId(txId);
      log.info("Calculate from tx id: {} last epoch {}", txId, lastEpoch.getNo());
      AdaPots adaPots;
      if (lastEpoch.getEra() == EraType.BYRON) {
        // if last epoch was byron then it will have no treasury, reward, deposit, fee (for reward calculation)
        adaPots =
            AdaPots.builder()
                .blockId(block.getId())
                .block(block)
                .epochNo(epochNo)
                .deposits(BigInteger.ZERO)
                .fees(BigInteger.ZERO)
                .reserves(RewardConstants.TOTAL_ADA.subtract(totalAdaSupplyOnChain))
                .utxo(totalAdaSupplyOnChain)
                .treasury(BigInteger.ZERO)
                .slotNo(block.getSlotNo())
                .rewards(BigInteger.ZERO)
                .build();
      } else {
        BigInteger fee = lastEpoch.getFees();
        var lastAdaPotsTxId = txService.getTxIdAdaPotsOfEpoch(lastEpochNo);
        BigInteger deposit = txRepository.getDepositFromTxIdToTxId(lastAdaPotsTxId, txId);
        if (Objects.isNull(fee)) {
          fee = BigInteger.ZERO;
        }
        if (Objects.isNull(deposit)) {
          deposit = BigInteger.ZERO;
        }

        var blockFee = txRepository.getFeeOfBlock(block.getId());
        fee = addLovelace(fee, blockFee);

        var lastAdaPots = getAdaPotByEpochNo(lastEpochNo);
        if (Objects.isNull(lastAdaPots.getBlockId())) {
          throw new PreviousAdaPotsNotFoundException(
              String.format("Information ada pots of epoch %s not found", lastEpochNo));
        }
        var reserve = lastAdaPots.getReserves();

        // reserve of previous epoch
        var rewardPotsFee = lastAdaPots.getFees();
        rewardPotsFee =
            subtractLovelace(rewardPotsFee, txRepository.getFeeOfBlock(lastAdaPots.getBlockId()));
        if (rewardPotsFee.compareTo(BigInteger.ZERO) < 0) {
          rewardPotsFee = BigInteger.ZERO;
        }

        var treasury =
            calculateTreasury(epochParam, reserve, rewardPotsFee, lastAdaPots.getTreasury());

        log.info(
            "Calculate treasury param: reserve {}, fee {}, last treasury {}",
            reserve,
            lastEpoch.getFees(),
            lastAdaPots.getTreasury());
        adaPots =
            AdaPots.builder()
                .blockId(block.getId())
                .block(block)
                .epochNo(epochNo)
                .deposits(deposit.add(lastAdaPots.getDeposits()))
                .fees(fee)
                .rewards(BigInteger.ZERO)
                .utxo(totalAdaSupplyOnChain)
                .treasury(treasury)
                .slotNo(block.getSlotNo())
                .build();
      }

      long endTime = System.currentTimeMillis();
      long totalTime = endTime - startTime;
      log.warn(
          "Calculation ada pots time elapsed: {} ms, {} second(s)", totalTime, totalTime / 1000f);
      return adaPots;
    } else {
      log.warn("Found no epoch with number {}", epochNo);
      return null;
    }
  }

  public BigInteger calculateTreasury(
      EpochParam epochParam, BigInteger reserve, BigInteger rewardPotFee, BigInteger lastTreasury) {
    var rewardPot = calculateRewardPot(epochParam, reserve, rewardPotFee);
    var treasury =
        calculateWithRate(
            new BigDecimal(rewardPot).toBigInteger(), epochParam.getTreasuryGrowthRate());
    return treasury.add(lastTreasury);
  }

  public BigInteger calculateTreasuryWithEta(
      EpochParam epochParam,
      BigInteger reserve,
      BigInteger rewardPotFee,
      BigInteger lastTreasury,
      int totalBlockMinted) {
    var rewardPot = calculateRewardPot(epochParam, reserve, rewardPotFee);
    // addition treasury
    var slotPerEpoch = new BigDecimal(RewardConstants.EXPECTED_SLOT_PER_EPOCH);
    var eta =
        new BigDecimal(totalBlockMinted)
            .divide(
                BigDecimal.ONE
                    .subtract(new BigDecimal(epochParam.getDecentralisation().toString()))
                    .multiply(slotPerEpoch),
                30,
                RoundingMode.DOWN);
    rewardPot = new BigDecimal(rewardPot).multiply(eta).toBigInteger();
    var treasury =
        calculateWithRate(
            new BigDecimal(rewardPot).toBigInteger(), epochParam.getTreasuryGrowthRate());
    System.out.println(rewardPot);
    System.out.println("Addition treasury with eta: " + treasury);
    return treasury.add(lastTreasury);
  }

  @Override
  public void updateRefundDeposit(AdaPots adaPots, PoolRetiredReward poolRetiredReward)
      throws RefundConflictException, ApiException {

    var totalRefund =
        poolRetiredReward.getRefundRewards().stream()
            .map(Reward::getAmount)
            .reduce(BigInteger.ZERO, BigInteger::add);
    var additionRefund = getRefundRewardIsNotDuplicate(adaPots.getEpochNo(), totalRefund);
    var rewardRemain =
        rewardService.getTotalRemainRewardOfEpoch(adaPots.getEpochNo()).add(additionRefund);
    adaPots.setRewards(rewardRemain);

    var deposit =
        adaPots
            .getDeposits()
            .subtract(poolRetiredReward.getAdditionTreasury())
            .subtract(totalRefund);
    adaPots.setDeposits(deposit);
    // TODO this part is currently blocked by an accurate treasury calculation
    // var treasury = adaPots.getTreasury().add(poolRetiredReward.getAdditionTreasury());
    // adaPots.setTreasury(treasury);
    updateTreasury(adaPots);
    updateReserve(adaPots);
  }

  public void updateTreasury(AdaPots adaPots) throws ApiException {
    var result =
        koiosClient
            .networkService()
            .getHistoricalTokenomicStatsByEpoch(adaPots.getEpochNo())
            .getValue();
    adaPots.setRewards(new BigInteger(result.getReward()));
    adaPots.setTreasury(new BigInteger(result.getTreasury()));
  }

  public void updateReserve(AdaPots adaPots) {
    var reserve =
        RewardConstants.TOTAL_ADA
            .subtract(adaPots.getUtxo())
            .subtract(adaPots.getFees())
            .subtract(adaPots.getDeposits())
            .subtract(adaPots.getTreasury())
            .subtract(adaPots.getRewards());
    adaPots.setReserves(reserve);
  }

  private BigInteger getRefundRewardIsNotDuplicate(int epoch, BigInteger calculationRefund)
      throws RefundConflictException {
    var currentRefundAmount = rewardService.getTotalRewardOfEpochByType(epoch, RewardType.REFUND);
    if (!currentRefundAmount.equals(BigInteger.ZERO)
        && !calculationRefund.equals(currentRefundAmount)) {
      throw new RefundConflictException(
          String.format(
              "Epoch %d Calculation refund reward %s conflict with current refund in db %s",
              epoch, calculationRefund, currentRefundAmount));
    }
    if (currentRefundAmount.equals(BigInteger.ZERO)) {
      return calculationRefund;
    }
    return BigInteger.ZERO;
  }

  @Override
  public int getLastEpochHasUpdated() {
    int epochNo = adaPotsRepository.getTheLatestEpochHasAdaPots().orElse(0);
    if (epochNo == 0) {
      Pageable pageable = PageRequest.of(0, 1);
      var epoch = epochRepository.getNextEpochByEraTypeGreater(EraType.BYRON, pageable);
      if (epoch.isEmpty()) {
        log.warn("Found no epoch has era greater than byron");
        // set max value for epoch no, so end epoch schedule won't update anything
        epochNo = Integer.MAX_VALUE;
      } else {
        var e = epoch.get(0);
        epochNo = e.getNo() - 1; // It will take snapshot balance before first shelley era 2 epoch
      }
    }

    return epochNo;
  }

  @Override
  public void save(AdaPots adaPots) {
    adaPotsRepository.save(adaPots);
  }

  private BigInteger addLovelace(BigInteger result, BigInteger addition) {
    if (Objects.isNull(result)) {
      result = BigInteger.ZERO;
    }
    if (Objects.isNull(addition)) {
      addition = BigInteger.ZERO;
    }
    return result.add(addition);
  }

  private BigInteger subtractLovelace(BigInteger result, BigInteger subtraction) {
    if (Objects.isNull(result)) {
      result = BigInteger.ZERO;
    }
    if (Objects.isNull(subtraction)) {
      subtraction = BigInteger.ZERO;
    }
    return result.subtract(subtraction);
  }

  public BigInteger calculateRewardPot(EpochParam epochParam, BigInteger reserve, BigInteger fee) {
    var rewardPot = calculateWithRate(reserve, epochParam.getMonetaryExpandRate());
    return rewardPot.add(fee);
  }

  private BigInteger calculateWithRate(BigInteger no, Double rate) {
    BigDecimal noDecimal = new BigDecimal(no);
    return noDecimal.multiply(new BigDecimal(rate)).toBigInteger();
  }

  private AdaPots getAdaPotByEpochNo(int epochNo) {
    return adaPotsRepository
        .getAdaPotsByEpochNo(epochNo)
        .orElse(
            AdaPots.builder()
                .treasury(BigInteger.ZERO)
                .deposits(BigInteger.ZERO)
                .reserves(BigInteger.ZERO)
                .build());
  }
}
