package org.cardanofoundation.rewards.converter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;

import org.cardanofoundation.rewards.common.entity.RewardType;
import org.cardanofoundation.rewards.common.StakingReward;
import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.common.entity.Reward;
import org.cardanofoundation.rewards.service.impl.HardForkServiceImpl;
import org.cardanofoundation.rewards.util.JsonConverter;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.cardanofoundation.rewards.repository.PoolHashRepository;
import org.cardanofoundation.rewards.repository.StakeAddressRepository;

@ExtendWith(MockitoExtension.class)
public class JsonConverterTest {

  @Mock PoolHashRepository poolHashRepository;

  @Mock StakeAddressRepository stakeAddressRepository;

  HardForkServiceImpl hardForkService;

  @Test
  void Test_convertJsonToReward() throws FileNotFoundException {
    BufferedReader br =
        new BufferedReader(
            new FileReader("./src/test/resources/reward/epoch211/pool216/reward.json"));
    ArrayList<Reward> rewards = JsonConverter.convertJsonToArrayList(br, Reward.class);
    Assertions.assertEquals(128, rewards.size());
    for (Object r : rewards) {
      Assertions.assertEquals(r.getClass(), Reward.class);
    }
  }

  @Test
  void Test_convertJsonToEpochStake() {
    String json =
        "[\n"
            + "  {\n"
            + "    \"stakeAddressId\": 1,\n"
            + "    \"amount\": 100,\n"
            + "    \"poolId\": 30\n"
            + "  }]";
    BufferedReader br = new BufferedReader(new StringReader(json));
    ArrayList<EpochStake> epochStakes = JsonConverter.convertJsonToArrayList(br, EpochStake.class);
    Assertions.assertEquals(1, epochStakes.size());
    Assertions.assertEquals(1, epochStakes.get(0).getStakeAddressId());
    Assertions.assertEquals(BigInteger.valueOf(100), epochStakes.get(0).getAmount());
    Assertions.assertEquals(30, epochStakes.get(0).getPoolId());
  }

  @Test
  void Test_convertJsonToStakingReward() {
    String json =
        "[\n"
            + "    {\n"
            + "        \"stakeAddress\": \"stake1uyypcwlvw4rcg009lhm50z0xf7cw2zhev7heljd9m2yng8sx6v60t\",\n"
            + "        \"poolId\": \"pool155cu3s06js3llfh07vnzepatfuvt3s5s7uwldt9rzz55wz7pugt\",\n"
            + "        \"amount\": 2004399,\n"
            + "        \"rewardType\": \"MEMBER\",\n"
            + "        \"earnedEpoch\": 215,\n"
            + "        \"spendableEpoch\": 217\n"
            + "    }\n"
            + "]";

    BufferedReader br = new BufferedReader(new StringReader(json));
    ArrayList<StakingReward> stakingRewards =
        JsonConverter.convertJsonToArrayList(br, StakingReward.class);
    Assertions.assertEquals(1, stakingRewards.size());
    Assertions.assertEquals(
        "stake1uyypcwlvw4rcg009lhm50z0xf7cw2zhev7heljd9m2yng8sx6v60t",
        stakingRewards.get(0).getStakeAddress());
    Assertions.assertEquals(
        "pool155cu3s06js3llfh07vnzepatfuvt3s5s7uwldt9rzz55wz7pugt",
        stakingRewards.get(0).getPoolId());
    Assertions.assertEquals(BigInteger.valueOf(2004399), stakingRewards.get(0).getAmount());
    Assertions.assertEquals(RewardType.MEMBER, stakingRewards.get(0).getRewardType());
    Assertions.assertEquals(215, stakingRewards.get(0).getEarnedEpoch());
    Assertions.assertEquals(217, stakingRewards.get(0).getSpendableEpoch());
  }

  //  @Test
  //  void Test_convertRewardOfEpoch215() {
  //    hardForkService = new HardForkServiceImpl(poolHashRepository, stakeAddressRepository);
  //    PoolHash poolReward =
  //        PoolHash.builder()
  //            .id(1L)
  //            .view("pool18v9r8afalh50l4lstct2awdc3zspnvurcs7t45nv29uc2mnxc6c")
  //            .build();
  //    StakeAddressAndIdProjection stakeAddress =
  //        StakeAddressAndIdProjection.builder()
  //            .view("stake1uypsj0cxzcrdzqzmxftle9umrrljw4yxmhlcxmk9x9a27vs4ngvyt")
  //            .id(2L)
  //            .build();
  //
  //    Mockito.when(poolHashRepository.findByViewIn(Mockito.anyCollection()))
  //        .thenReturn(List.of(poolReward));
  //    Mockito.when(stakeAddressRepository.findByAddressesViewIn(Mockito.anySet()))
  //        .thenReturn(List.of(stakeAddress));
  //
  //    List<Reward> rewards =
  //        hardForkService.getRewardEpoch215().values().stream()
  //            .flatMap(Collection::stream)
  //            .collect(Collectors.toList());
  //    Assertions.assertEquals(1, rewards.size());
  //    Assertions.assertEquals(1, rewards.get(0).getPoolId());
  //    Assertions.assertEquals(2, rewards.get(0).getStakeAddressId());
  //  }
}
