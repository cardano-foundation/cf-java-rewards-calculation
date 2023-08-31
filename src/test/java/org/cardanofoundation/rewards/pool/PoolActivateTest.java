package org.cardanofoundation.rewards.pool;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;

import org.cardanofoundation.rewards.common.entity.RewardType;
import org.cardanofoundation.rewards.common.entity.Reward;
import org.cardanofoundation.rewards.projection.PoolConfigProjection;
import org.cardanofoundation.rewards.service.PoolRetireService;
import org.cardanofoundation.rewards.service.PoolService;
import org.cardanofoundation.rewards.service.PoolUpdateService;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;

import org.cardanofoundation.rewards.repository.EpochStakeRepository;
import org.cardanofoundation.rewards.repository.PoolHashRepository;
import org.cardanofoundation.rewards.repository.RewardRepository;

// Use data of db sync
@SpringBootTest
@Disabled
public class PoolActivateTest {

  @Autowired
  PoolUpdateService poolUpdateService;

  @Autowired RewardRepository rewardRepository;

  @Autowired
  PoolRetireService poolRetireService;

  @Autowired PoolHashRepository poolHashRepository;

  @Autowired
  PoolService poolService;

  @Autowired EpochStakeRepository epochStakeRepository;

  @Test
  void Test_getAllActivatePoolConfig() {
    for (int i = 245; i < 246; i++) {
      var poolConfigs = poolUpdateService.findAllActivePoolConfig(i);
      Set<Long> poolIds =
          poolConfigs.stream().map(PoolConfigProjection::getPoolId).collect(Collectors.toSet());
      Set<RewardType> types = new HashSet<>();
      types.add(RewardType.LEADER);
      types.add(RewardType.MEMBER);
      var poolIdHasReward = rewardRepository.getPoolIdHasRewardInAnEpochByTypes(i, types);
      TreeSet<Long> poolIdsDoNotGetReward =
          poolIdHasReward.stream()
              .filter(id -> !poolIds.contains(id))
              .collect(Collectors.toCollection(TreeSet::new));
      if (!poolIdsDoNotGetReward.isEmpty()) {
        System.out.println(
            "Epoch has conflict: "
                + i
                + ", reward pool total size: "
                + poolIdHasReward.size()
                + ", pool not get reward size: "
                + poolIdsDoNotGetReward.size());
        poolIdsDoNotGetReward.forEach(id -> System.out.print(id + ", "));
        System.out.println();
      }
    }
  }

  @Test
  void Test_refundOfRetiredPool() {
    var epochException = new HashSet<>();
    for (int i = 365; i < 405; i++) {
      var refundRewards = new ArrayList<>(poolRetireService.getRefundRewards(i).getRefundRewards());
      Set<RewardType> types = new HashSet<>();
      types.add(RewardType.REFUND);
      var rewards = rewardRepository.getRewardInAnEpochByTypes(i, types);
      System.out.println("Epoch: " + i);
      if (!refundRewards.isEmpty() && !rewards.isEmpty()) {
        rewards.sort(Comparator.comparing(Reward::getPoolId));
        refundRewards.sort(Comparator.comparing(Reward::getPoolId));
        if (rewards.size() > refundRewards.size()) {
          List<Reward> expectedRewards = getRewardNotContain(rewards, refundRewards);
          System.out.println("Expected rewards");
          printReward(expectedRewards);
        } else if (rewards.size() < refundRewards.size()) {
          List<Reward> actual = getRewardNotContain(refundRewards, rewards);
          System.out.println("Actual rewards");
          printReward(actual);
        }
      }
      if (epochException.contains(i)) {
        continue;
      }
      if (rewards.isEmpty()) {
        assertEquals(rewards, refundRewards);
      } else {
        for (int j = 0; j < rewards.size(); j++) {
          assertEquals(rewards.get(j).getPoolId(), refundRewards.get(j).getPoolId());
          assertEquals(
              rewards.get(j).getStakeAddressId(), refundRewards.get(j).getStakeAddressId());
        }
      }
    }
  }

  private List<Reward> getRewardNotContain(List<Reward> expected, List<Reward> actual) {
    var mActual =
        actual.stream().collect(Collectors.toMap(Reward::getPoolId, Reward::getStakeAddressId));
    return expected.stream()
        .filter(reward -> !mActual.containsKey(reward.getPoolId()))
        .collect(Collectors.toList());
  }

  private void printReward(List<Reward> rewards) {
    rewards.forEach(
        reward ->
            System.out.print(
                "Pool Id: "
                    + reward.getPoolId()
                    + ", Address id: "
                    + reward.getStakeAddressId()
                    + "; "));
    System.out.println();
  }

  @Test
  void Test_RetiredPoolWithActivatePool() {
    var refundRewards = new ArrayList<>(poolRetireService.getRefundRewards(306).getRefundRewards());
    printReward(refundRewards);
    System.out.println();
  }

  @Test
  void Text_X() {
    for (int i = 211; i < 216; i++) {
      var pool = poolUpdateService.findAllActivePoolConfig(i);
      System.out.println(pool.size());
      pool.stream()
          .map(poolConfigProjection -> poolConfigProjection.getPoolId())
          .collect(Collectors.toSet());
      var pIds =
          pool.stream()
              .filter(poolConfigProjection -> poolConfigProjection.getPoolId() == 10)
              .map(poolConfigProjection -> poolConfigProjection.getPoolId())
              .collect(Collectors.toList());
      assertTrue(pIds.contains(10L));
    }
  }

  @Test
  void Test_PoolNotActivated() {
    Set<String> poolHashesNotActivate =
        new HashSet<>(
            Arrays.asList(
                "pool1g60m45m23f5vta30x5z7e0n2gc02yc4wyz6darfeluy2kgu65fa",
                "pool1txtq2n2jefg7rqja2ls69qfc7tke2ll9xvxd3dwj3tcfstmjgsu",
                "pool1qqqg664ad0cd47787e9ksfnl2utwrxfdp6z9av3dq5r9k6qfurw",
                "pool1s7xvt9453rruhfdc90dy02xue82mnv2lzyqgzxx8ssmcq4fc3kj",
                "pool1qeyjycp9ef0drrzaq3u9ylwclqa56zl5n4yd75txw92csug3mzn",
                "pool13d2tchandz87ux0pgkxjdxd62zkuj872tx5stpcth8f6xequ579",
                "pool1m06tlj2ykawzvweacgmhxj43hykczgfuynk2lqzxvshm5lq2lyq",
                "pool1c02sa4dg5m2dq7lspgwwea95j3zr7hzl4wsuh0a2tcly60ua59n",
                "pool17w80nr9ujut4thwanfjstnvuwtwwk8luyln6vvrlqtv7sz3szeq",
                "pool1352aq7dqpuvp3qj8juy82paappffa8q84nscmcvnwhal7ejl60z",
                "pool1xh4gz3yu408dv4u8km24kwdg7r50nl2qwu9we6tx4cuc7qn2jfx",
                "pool13pgg603z5qz9q9e33lfpgc4d0p6w37t9rd47u29cr7rex87wcs6"));

    var poolIds = poolService.getPoolCanStakeFromEpoch(209);
    var poolIdNotActivate = poolHashRepository.findIdByHashes(poolHashesNotActivate);
    System.out.println("Pool id size: " + poolIds.size());
    poolIdNotActivate.forEach(
        id -> assertFalse(poolIds.contains(id), String.format("Pool Id conflict %s", id)));
    var poolStakeIdInEpoch = epochStakeRepository.getPoolIdInEpoch(211);
    System.out.println("Pool id stake size: " + poolStakeIdInEpoch.size());
    poolStakeIdInEpoch.stream()
        .filter(pId -> !poolIds.contains(pId))
        .forEach(id -> System.out.println("Pool id stake of db sync: " + id));

    assertTrue(poolIds.containsAll(poolStakeIdInEpoch));
  }
}
