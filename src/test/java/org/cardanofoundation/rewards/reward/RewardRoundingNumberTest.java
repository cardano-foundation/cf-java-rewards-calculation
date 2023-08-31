package org.cardanofoundation.rewards.reward;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import org.cardanofoundation.rewards.common.entity.RewardType;
import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.repository.PoolOwnerRepository;
import org.cardanofoundation.rewards.repository.RewardRepository;
import org.cardanofoundation.rewards.repository.SlotLeaderRepository;
import org.cardanofoundation.rewards.repository.WithdrawalRepository;
import org.cardanofoundation.rewards.service.*;
import org.cardanofoundation.rewards.service.impl.RewardParam;
import org.cardanofoundation.rewards.service.impl.RewardServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.cardanofoundation.rewards.common.entity.EpochParam;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Disabled
public class RewardRoundingNumberTest {

  @Mock
  EpochParamService epochParamService;
  @Mock
  RewardRepository rewardRepository;

  @Mock
  WithdrawalRepository withdrawalRepository;

  @Mock
  PoolUpdateService poolUpdateService;

  @Mock
  EpochStakeService epochStakeService;

  @Mock
  PoolOwnerRepository poolOwnerRepository;

  @Mock
  TxService txService;

  @Mock
  StakeAddressService stakeAddressService;

  @Mock PoolService poolService;

  @InjectMocks
  RewardServiceImpl rewardService;

  @Mock
  SlotLeaderRepository slotLeaderRepository;

  @BeforeEach
  void setup() {
    rewardService =
        new RewardServiceImpl(
            epochParamService,
            rewardRepository,
            withdrawalRepository,
            poolUpdateService,
            epochStakeService,
            poolOwnerRepository,
            txService,
            stakeAddressService,
            poolService,
            slotLeaderRepository);
  }

  @Test
  // stake key: stake1u8dmqlfv95cyr9u7gskm03cw4s7vm06jq0kctse4klk4fycpm5q4j
  // pool: pool1mxqjlrfskhd5kql9kak06fpdh8xjwc76gec76p3taqy2qmfzs5z
  void Test_CalculateMemberRewardOfPool531OfAddr53004InEpoch212() {
    var reserves = new BigInteger("13247093198353459");
    var fees = new BigInteger("5578218279");
    var totalAda = new BigDecimal(RewardConstants.TOTAL_ADA).subtract(new BigDecimal(reserves));

    var relativeStakeOfMember =
        new BigDecimal("221527836462686").divide(totalAda, 30, RoundingMode.DOWN);
    var relativePledgeStakeOfPoolOwner =
        new BigDecimal("5000000000000").divide(totalAda, 30, RoundingMode.DOWN);
    var stakeOfPoolInEpoch212 = new BigDecimal("229876590276640");
    var relativeStakeOfPool = stakeOfPoolInEpoch212.divide(totalAda, 30, RoundingMode.DOWN);

    EpochParam epochParam =
        EpochParam.builder()
            .influence(Double.valueOf("0.3"))
            .optimalPoolCount(150)
            .treasuryGrowthRate(Double.valueOf("0.2"))
            .monetaryExpandRate(Double.valueOf("0.003"))
            .build();

    var poolReward =
        rewardService.calculatePoolReward(
            epochParam,
            reserves,
            fees,
            relativeStakeOfPool,
            relativePledgeStakeOfPoolOwner,
            BigDecimal.ONE,
            21600);

    PoolConfigProjectionTest poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(531L)
            .rewardAddressId(37734L)
            .fixedCost(BigInteger.valueOf(340000000))
            .pledge(BigInteger.valueOf(5000000000000L))
            .margin(Double.valueOf("0.07"))
            .build();

    RewardParam param =
        RewardParam.builder()
            .stakeAddressId(53004L)
            .relativeStake(relativeStakeOfMember)
            .poolReward(new BigDecimal(poolReward.toBigInteger()))
            .relativeStakeOfPool(relativeStakeOfPool)
            .poolConfigProjection(poolConfigProjection)
            .epochNo(212)
            .build();
    var reward = rewardService.calculateMemberReward(param);
    Assertions.assertEquals(53004L, reward.getStakeAddressId());
    Assertions.assertEquals(RewardType.MEMBER, reward.getType());
    Assertions.assertEquals(531L, reward.getPoolId());
    Assertions.assertEquals(212, reward.getEarnedEpoch());
    Assertions.assertEquals(214, reward.getSpendableEpoch());
    Assertions.assertEquals(BigInteger.valueOf(146872718436L), reward.getAmount());
  }
}
