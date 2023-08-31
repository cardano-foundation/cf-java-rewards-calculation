package org.cardanofoundation.rewards.reward;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.common.entity.*;
import org.cardanofoundation.rewards.projection.PoolConfigProjection;
import org.cardanofoundation.rewards.repository.*;
import org.cardanofoundation.rewards.service.*;
import org.cardanofoundation.rewards.service.impl.PoolServiceImpl;
import org.cardanofoundation.rewards.service.impl.RewardServiceImpl;
import org.cardanofoundation.rewards.util.JsonConverter;
import org.junit.jupiter.api.Disabled;
import org.springframework.data.util.Pair;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RewardTest {

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

  @Mock
  PoolRetireRepository poolRetireRepository;

  @Mock
  PoolUpdateRepository poolUpdateRepository;

  @Mock
  SlotLeaderRepository slotLeaderRepository;

  @Mock
  EpochParamRepository epochParamRepository;

  @Mock
  EpochRepository epochRepository;

  private RewardServiceImpl rewardService;

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

  private void testReward(
      BigInteger reserve,
      BigInteger fees,
      EpochParam epochParam,
      List<EpochStake> epochStakes,
      PoolConfigProjection poolConfigProjection,
      List<Reward> rewards,
      Set<Long> poolOwnerIds,
      Set<Long> stakeAddressIds,
      BigDecimal poolPerformance,
      int blockMinted) {
    System.out.println("Pool performance: " + poolPerformance);
    System.out.println("epoch stake size: " + epochStakes.size());
    System.out.println("reward size: " + rewards.size());

    var rewardResult =
        rewardService.calculateRewardsOfAPool(
            poolConfigProjection,
            reserve,
            fees,
            epochParam,
            epochStakes,
            epochParam.getEpochNo(),
            poolOwnerIds,
            stakeAddressIds,
            poolPerformance,
            blockMinted);
    var mRewards =
        rewards.stream()
            .collect(
                Collectors.toMap(
                    reward -> Pair.of(reward.getStakeAddressId(), reward.getType()),
                    Function.identity()));
    AtomicInteger total = new AtomicInteger();
    rewardResult.forEach(
        reward -> {
          if (Objects.isNull(mRewards.get(Pair.of(reward.getStakeAddressId(), reward.getType())))) {
            log.warn("Found no reward expect for addr {}", reward.getStakeAddressId());
          } else {
            var calculationReward = reward.getAmount();
            var dbSyncReward =
                mRewards.get(Pair.of(reward.getStakeAddressId(), reward.getType())).getAmount();
            log.info(
                "Addr id {}, type {}, actual {} , expect {}, different {} percent(actual/expect) {}%",
                reward.getStakeAddressId(),
                reward.getType(),
                calculationReward,
                dbSyncReward,
                calculationReward.subtract(dbSyncReward),
                new BigDecimal(calculationReward)
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(dbSyncReward), 5, RoundingMode.DOWN));
            assertEquals(dbSyncReward, calculationReward);
            if (dbSyncReward.equals(calculationReward)) {
              total.getAndIncrement();
            }
            if (reward.getType() == RewardType.LEADER) {
              log.debug(
                  "LEADER Addr id {}, type {}, actual {} , expect {}",
                  reward.getStakeAddressId(),
                  reward.getType(),
                  reward.getAmount(),
                  mRewards.get(Pair.of(reward.getStakeAddressId(), reward.getType())).getAmount());
            }
          }
        });
    System.out.println("Size: " + rewardResult.size() + ",Total equal: " + total);
  }

  @Test
  void Test_cal() {
    var rewardMember = new BigDecimal(379325374L);
    var relativeStakeOfMember = new BigDecimal("0.000015730315772473");
    var relativeStakeOfPool = new BigDecimal("0.001795639230812930");
    var poolMargin = new BigDecimal("0.009");
    var poolFixedCost = new BigDecimal(340000000);
    System.out.println(
        poolMemberToPoolReward(
            rewardMember, relativeStakeOfMember, relativeStakeOfPool, poolMargin, poolFixedCost));
  }

  public BigDecimal poolMemberToPoolReward(
      BigDecimal rewardMember,
      BigDecimal relativeStakeOfMember,
      BigDecimal relativeStakeOfPool,
      BigDecimal poolMargin,
      BigDecimal poolFixedCost) {
    return rewardMember
        .multiply(relativeStakeOfPool)
        .divide(relativeStakeOfMember, 30, RoundingMode.DOWN)
        .divide(BigDecimal.ONE.subtract(poolMargin), 30, RoundingMode.DOWN)
        .add(poolFixedCost);
  }

  @Test
  void Test_CalculateRewardOfPool216InEpoch211() throws IOException {
    BigInteger reserve = BigInteger.valueOf(13262280841681299L);
    BigInteger fees = BigInteger.valueOf(6517886228L);
    EpochParam epochParam =
        EpochParam.builder()
            .epochNo(211)
            .influence(0.3)
            .optimalPoolCount(150)
            .decentralisation(Double.valueOf("1"))
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch211/pool216/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch211/pool216/reward.json", Reward.class);
    Map<Long, PoolOwner> poolOwnerMap = new HashMap<>();
    poolOwnerMap.put(3063L, new PoolOwner());
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(216L)
            .pledge(BigInteger.valueOf(500000000000L))
            .rewardAddressId(3063L)
            .margin(0.009)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    var totalStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    System.out.println("totalStake: " + totalStake);
    assertEquals(BigInteger.valueOf(51673146832377L), totalStake);
    var poolOwnerIds = new HashSet<>(List.of(3063L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    var totalActivateStake = new BigInteger("10177811974822904");
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        BigDecimal.ONE,
        21600);
  }

  @Test
  void Test_CalculateRewardOfPool51InEpoch211() throws IOException {
    BigInteger reserve = BigInteger.valueOf(13262280841681299L);
    BigInteger fees = BigInteger.valueOf(6517886228L);
    EpochParam epochParam =
        EpochParam.builder()
            .epochNo(211)
            .influence(0.3)
            .optimalPoolCount(150)
            .decentralisation(Double.valueOf("1"))
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch211/pool51/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch211/pool51/reward.json", Reward.class);
    Map<Long, PoolOwner> poolOwnerMap = new HashMap<>();
    poolOwnerMap.put(1447L, new PoolOwner());
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(51L)
            .pledge(BigInteger.valueOf(425000000000L))
            .margin((double) 0)
            .rewardAddressId(1447L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    var totalStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(List.of(1447L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        BigDecimal.ONE,
        21600);
  }

  @Test
  @Disabled
    // pool: pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt
  void Test_CalculateRewardOfPool1InEpoch213() throws IOException {
    BigInteger reserve = BigInteger.valueOf(13230232787944838L);
    BigInteger fees = BigInteger.valueOf(7100593256L);
    EpochParam epochParam =
        EpochParam.builder()
            .epochNo(213)
            .influence(0.3)
            .optimalPoolCount(150)
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0.78"))
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch213/pool1/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch213/pool1/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(1L)
            .pledge(BigInteger.valueOf(450000000000L))
            .margin(0.015)
            .rewardAddressId(465L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 13;
    BigInteger totalEpochStake = new BigInteger("12758829109784350");
    // int totalBlock = 21442;
    var totalBlock = 4625;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(List.of(465L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(25906028308939L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  @Test
  void Test_GetPoolPerformance() {
    int blockPoolHasMinted = 13;
    BigInteger totalEpochStake = new BigInteger("12758829109784350");
    BigInteger poolStake = new BigInteger("25906028308939");
    int totalBlock = 21600;
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    assertEquals(new BigDecimal("0.296414596464228188354493042058"), poolPerformance);
  }

  @Test
    // pool: pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt
  void Test_CalculateRewardOfPool1InEpoch260() throws IOException {
    BigInteger reserve = BigInteger.valueOf(12527064254027631L);
    BigInteger fees = BigInteger.valueOf(46907924914L);
    EpochParam epochParam =
        EpochParam.builder()
            .epochNo(260)
            .influence(0.3)
            .optimalPoolCount(500)
            .monetaryExpandRate(0.003)
            .decentralisation(Double.valueOf("0"))
            .treasuryGrowthRate(0.2)
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch260/pool1/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch260/pool1/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(1L)
            .pledge(BigInteger.valueOf(470000000000L))
            .margin(0.009)
            .rewardAddressId(2383L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 55;
    BigInteger totalEpochStake = new BigInteger("22796009141134396");
    int totalBlock = 21047;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(Arrays.asList(2383L, 353018L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(63299729869094L), poolStake);
    BigDecimal d = BigDecimal.ZERO;
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    var totalActivateStake = new BigInteger("22796009141134396");
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  @Test
  @Disabled
    // pool: pool1kchver88u3kygsak8wgll7htr8uxn5v35lfrsyy842nkscrzyvj
    // d: 0
  void Test_CalculateRewardOfPool184DbSyncInEpoch260() throws IOException {
    BigInteger reserve = BigInteger.valueOf(12527064254027631L);
    BigInteger fees = BigInteger.valueOf(46907924914L);
    EpochParam epochParam =
        EpochParam.builder()
            .epochNo(260)
            .influence(0.3)
            .optimalPoolCount(500)
            .monetaryExpandRate(0.003)
            .decentralisation(Double.valueOf("0"))
            .treasuryGrowthRate(0.2)
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch260/pool184/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch260/pool184/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(184L)
            .pledge(BigInteger.valueOf(100000000000L))
            .margin(0.018)
            .rewardAddressId(311489L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 73;
    BigInteger totalEpochStake = new BigInteger("22796009141134396");
    // int totalBlock = 21600;
    int totalBlock = 21047;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(Arrays.asList(307097L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(61143124051642L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    var totalActivateStake = new BigInteger("24723813844048216");
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  // pool: pool1zk6uxgptztrxxamzd0ugrtu84xg8mjymd2xcdvxqazpruqt9r3x
  // d: 0
  @Test
  void Test_CalculateRewardOfPool1466DbSyncInEpoch400() throws IOException {
    BigInteger reserve = BigInteger.valueOf(9391113276049044L);
    BigInteger fees = BigInteger.valueOf(107737939769L);
    EpochParam epochParam =
        EpochParam.builder()
            .epochNo(400)
            .influence(0.3)
            .optimalPoolCount(500)
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0"))
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch400/pool1466/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch400/pool1466/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(1466L)
            .pledge(BigInteger.valueOf(300000000000L))
            .margin(Double.valueOf(0.009))
            .rewardAddressId(215860L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 12;
    BigInteger totalEpochStake = new BigInteger("24723813844048216");
    int totalBlock = 21062;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(Arrays.asList(215860L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(7300516572283L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    var totalActivateStake = new BigInteger("24723813844048216");
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  // pool: pool1fh4sh2telea0tfdy39h0drx6uq566yt9gf24edpz7335sclx39z
  // d: 0
  @Test
  void Test_CalculateRewardOfPool30DbSyncInEpoch400() throws IOException {
    BigInteger reserve = BigInteger.valueOf(9391113276049044L);
    BigInteger fees = BigInteger.valueOf(107737939769L);
    EpochParam epochParam =
        EpochParam.builder()
            .epochNo(400)
            .influence(0.3)
            .optimalPoolCount(500)
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0"))
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch400/pool30/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch400/pool30/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(30L)
            .pledge(BigInteger.valueOf(550000000000L))
            .margin(Double.valueOf(0.02))
            .rewardAddressId(416862L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 9;
    BigInteger totalEpochStake = new BigInteger("24723813844048216");
    int totalBlock = 21062;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(Arrays.asList(394727L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(12118614771552L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  // pool: pool1mxqjlrfskhd5kql9kak06fpdh8xjwc76gec76p3taqy2qmfzs5z
  // d: 0.74
  @Test
  @Disabled
  void Test_CalculateRewardOfPool531DbSyncInEpoch215() throws IOException {
    BigInteger reserve = BigInteger.valueOf(13195031638588164L);
    BigInteger fees = BigInteger.valueOf(8110049274L);
    EpochParam epochParam =
        EpochParam.builder()
            .influence(0.3)
            .epochNo(215)
            .optimalPoolCount(150)
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0.74"))
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch215/pool531/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch215/pool531/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(531L)
            .pledge(BigInteger.valueOf(5000000000000L))
            .margin(Double.valueOf(0.07))
            .rewardAddressId(70303L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 115;
    BigInteger totalEpochStake = new BigInteger("13864141914846088");
    int totalBlock = 5710;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(Arrays.asList(70047L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(239441181404825L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  // pool: pool1a2nh3ktswllhwf07fjahpdg5mpqyq7j950pyftq9765r6t4cefl
  // d: 0.74
  @Test
  void Test_CalculateRewardOfPool179DbSyncInEpoch215() throws IOException {
    BigInteger reserve = BigInteger.valueOf(13195031638588164L);
    BigInteger fees = BigInteger.valueOf(8110049274L);
    EpochParam epochParam =
        EpochParam.builder()
            .influence(0.3)
            .epochNo(215)
            .optimalPoolCount(150)
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0.74"))
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch215/pool179/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch215/pool179/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(179L)
            .pledge(BigInteger.valueOf(500000000000L))
            .margin(Double.valueOf(0.009))
            .rewardAddressId(2501L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 82;
    BigInteger totalEpochStake = new BigInteger("13864141914846088");
    int totalBlock = 5710;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    Set<Long> poolOwnerIds = new HashSet<>(List.of(2501L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(230737446437391L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  // pool: pool1mxqjlrfskhd5kql9kak06fpdh8xjwc76gec76p3taqy2qmfzs5z
  // d: 0.44
  @Test
  void Test_CalculateRewardOfPool531DbSyncInEpoch230() throws IOException {
    BigInteger reserve = BigInteger.valueOf(12901374614727880L);
    BigInteger fees = BigInteger.valueOf(7168655203L);
    EpochParam epochParam =
        EpochParam.builder()
            .influence(0.3)
            .epochNo(230)
            .optimalPoolCount(150)
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0.44"))
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch230/pool531/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch230/pool531/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(531L)
            .pledge(BigInteger.valueOf(5000000000000L))
            .margin(Double.valueOf(0.07))
            .rewardAddressId(70303L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 98;
    BigInteger totalEpochStake = new BigInteger("19636382936670747");
    int totalBlock = 11902;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(Arrays.asList(70047L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(172692505117720L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }

  @Test
  void Test_CalculateRewardOfPool531DbSyncInEpoch220() {
    BigInteger reserve = BigInteger.valueOf(13101550250680254L);
    BigInteger fees = BigInteger.valueOf(5135934788L);
    EpochParam epochParam =
        EpochParam.builder()
            .influence(0.3)
            .epochNo(220)
            .optimalPoolCount(150)
            .monetaryExpandRate(0.003)
            .treasuryGrowthRate(0.2)
            .decentralisation(Double.valueOf("0.64"))
            .build();
    List<EpochStake> epochStakes =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch220/pool256/epoch_stake.json", EpochStake.class);
    List<Reward> rewards =
        JsonConverter.convertFileJsonToArrayList(
            "./src/test/resources/reward/epoch220/pool256/reward.json", Reward.class);
    PoolConfigProjection poolConfigProjection =
        PoolConfigProjectionTest.builder()
            .poolId(256L)
            .pledge(BigInteger.valueOf(150000000000L))
            .margin(Double.valueOf(0.04))
            .rewardAddressId(5080L)
            .fixedCost(BigInteger.valueOf(340000000L))
            .build();
    int blockPoolHasMinted = 18;
    BigInteger totalEpochStake = new BigInteger("15859988643309526");
    int totalBlock = 7839;
    var poolStake =
        epochStakes.stream().map(EpochStake::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
    var poolOwnerIds = new HashSet<>(Arrays.asList(5597L));
    var stakeAddressIds =
        epochStakes.stream().map(EpochStake::getStakeAddressId).collect(Collectors.toSet());
    assertEquals(BigInteger.valueOf(16109672308775L), poolStake);
    var poolPerformance =
        poolService.getPoolPerformanceOfPool(
            poolStake, totalEpochStake, blockPoolHasMinted, totalBlock);
    testReward(
        reserve,
        fees,
        epochParam,
        epochStakes,
        poolConfigProjection,
        rewards,
        poolOwnerIds,
        stakeAddressIds,
        poolPerformance,
        totalBlock);
  }
}
