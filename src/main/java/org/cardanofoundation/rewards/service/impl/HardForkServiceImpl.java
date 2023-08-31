package org.cardanofoundation.rewards.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.common.entity.PoolHash;
import org.cardanofoundation.rewards.common.entity.Reward;
import org.cardanofoundation.rewards.common.entity.RewardType;
import org.cardanofoundation.rewards.common.entity.StakeAddress;
import org.cardanofoundation.rewards.constants.RewardConstants;
import org.springframework.stereotype.Service;

import org.cardanofoundation.rewards.common.MissingReward;
import org.cardanofoundation.rewards.repository.PoolHashRepository;
import org.cardanofoundation.rewards.repository.StakeAddressRepository;
import org.cardanofoundation.rewards.service.HardForkService;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class HardForkServiceImpl implements HardForkService {

  PoolHashRepository poolHashRepository;
  StakeAddressRepository stakeAddressRepository;

  @Override
  public Collection<Reward> handleRewardIssueForEachEpoch(
      int networkMagic, int epoch, Collection<Reward> rewards) {
    if (networkMagic == RewardConstants.MAINNET_NETWORK_MAGIC) {
      switch (epoch) {
        case 212:
          return removeRewardAddressDidNotReceiveInEpoch212(rewards);
        case 213:
          return removeRewardAddressDidNotReceiveInEpoch213(rewards);
        case 214:
          return removeRewardAddressDidNotReceiveInEpoch214(rewards);
        case 216:
          return removeRewardAddressDidNotReceiveInEpoch216(rewards);
        case 217:
          return removeRewardAddressDidNotReceiveInEpoch217(rewards);
        case 218:
          return removeRewardAddressDidNotReceiveInEpoch218(rewards);
        case 219:
          return removeRewardAddressDidNotReceiveInEpoch219(rewards);
        case 220:
          return removeRewardAddressDidNotReceiveInEpoch220(rewards);
        case 221:
          return removeRewardAddressDidNotReceiveInEpoch221(rewards);
        case 222:
          return removeRewardAddressDidNotReceiveInEpoch222(rewards);
        case 223:
          return removeRewardAddressDidNotReceiveInEpoch223(rewards);
        case 224:
          return removeRewardAddressDidNotReceiveInEpoch224(rewards);
        case 225:
          return removeRewardAddressDidNotReceiveInEpoch225(rewards);
        case 226:
          return removeRewardAddressDidNotReceiveInEpoch226(rewards);
        case 227:
          return removeRewardAddressDidNotReceiveInEpoch227(rewards);
        case 228:
          return removeRewardAddressDidNotReceiveInEpoch228(rewards);
        case 229:
          return removeRewardAddressDidNotReceiveInEpoch229(rewards);
        case 230:
          return removeRewardAddressDidNotReceiveInEpoch230(rewards);
        case 231:
          return removeRewardAddressDidNotReceiveInEpoch231(rewards);
      }
    }
    return rewards;
  }

  //    public Map<Long, List<Reward>> getRewardEpoch215() {
  //        List<StakingReward> stakingRewards =
  //                JsonConverter.convertFileJsonToArrayList(
  //                        "./src/main/resources/epoch215/reward.json", StakingReward.class);
  //
  //        // map pool hash to with pool id
  //        Set<String> poolHashes =
  //
  // stakingRewards.stream().map(StakingReward::getPoolId).collect(Collectors.toSet());
  //        Map<String, Long> mPools =
  //                poolHashRepository.findByViewIn(poolHashes).stream()
  //                        .collect(Collectors.toMap(PoolHash::getView, PoolHash::getId));
  //
  //        // map stake address view with stake address id
  //        Set<String> stakeAddresses =
  //
  // stakingRewards.stream().map(StakingReward::getStakeAddress).collect(Collectors.toSet());
  //
  //        Map<String, Long> mStakeAddresses =
  //                stakeAddressRepository.findByAddressesViewIn(stakeAddresses).stream()
  //                        .collect(
  //                                Collectors.toMap(
  //                                        StakeAddressAndIdProjection::getView,
  // StakeAddressAndIdProjection::getId));
  //
  //        return stakingRewards.stream()
  //                .map(
  //                        stakingReward ->
  //                                Reward.builder()
  //                                        .amount(stakingReward.getAmount())
  //                                        .type(stakingReward.getRewardType())
  //                                        .addr(
  //                                                StakeAddress.builder()
  //
  // .id(mStakeAddresses.get(stakingReward.getStakeAddress()))
  //                                                        .build())
  //
  // .stakeAddressId(mStakeAddresses.get(stakingReward.getStakeAddress()))
  //
  // .pool(PoolHash.builder().id(mPools.get(stakingReward.getPoolId())).build())
  //                                        .poolId(mPools.get(stakingReward.getPoolId()))
  //                                        .earnedEpoch(stakingReward.getEarnedEpoch())
  //                                        .spendableEpoch(stakingReward.getSpendableEpoch())
  //                                        .build())
  //                .filter(
  //                        reward ->
  //                                Objects.nonNull(reward.getPoolId()) &&
  // Objects.nonNull(reward.getStakeAddressId()))
  //                .collect(Collectors.groupingBy(Reward::getPoolId));
  //    }

  private Collection<Reward> modifyRewards(
      Collection<Reward> rewards, List<MissingReward> missingRewards) {
    // mapping stake address view  to a map has value is stakeId
    var mStakeAddressViews =
        missingRewards.stream().map(MissingReward::getStakeAddress).collect(Collectors.toSet());

    Map<String, Long> mStakeAddresses =
        stakeAddressRepository.findByViewIn(mStakeAddressViews).stream()
            .collect(Collectors.toMap(StakeAddress::getView, StakeAddress::getId));

    // mapping pool view  to a map has value is poolId
    var poolHashes =
        missingRewards.stream().map(MissingReward::getPoolHash).collect(Collectors.toSet());

    Map<String, Long> mPools =
        poolHashRepository.findByViewIn(poolHashes).stream()
            .collect(Collectors.toMap(PoolHash::getView, PoolHash::getId));
    missingRewards.forEach(
        missingReward -> {
          missingReward.setPoolId(mPools.get(missingReward.getPoolHash()));
          missingReward.setStakeAddressId(mStakeAddresses.get(missingReward.getStakeAddress()));
        });

    Set<MissingReward> sMissingReward = new HashSet<>(missingRewards);

    return rewards.stream()
        .filter(reward -> !sMissingReward.contains(convertRewardToRewardMissing(reward)))
        .collect(Collectors.toList());
  }

  public MissingReward convertRewardToRewardMissing(Reward reward) {
    return MissingReward.builder()
        .stakeAddressId(reward.getStakeAddressId())
        .poolId(reward.getPoolId())
        .rewardType(reward.getType())
        .build();
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch212(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99gkx28kt8n4nwygw5fqg4y2d5rc7pmlpj03vr6t8v5xecqwdemh")
            .poolHash("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch213(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9p26xk0cnjccs2an3qsm6fqca3uqp3e94l6dxu6pxvxkyqxa7q82")
            .poolHash("pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch214(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1ux0hx0hyhlplsrzewj6r3c64guymjqptw4qmt64c7yey6pcxzx895")
            .poolHash("pool1h0524mtazrjnzqh5e4u060jsfk8lpsqqjfpa5gygjwuhqu34wvt")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99gkx28kt8n4nwygw5fqg4y2d5rc7pmlpj03vr6t8v5xecqwdemh")
            .poolHash("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch216(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uyfmkfyp5arc4yfr7s0m3pc2adtrvmyhp279w9pcvzggkcgxzjd5y")
            .poolHash("pool1qqlf3epa7m8780p2jx5689u2ql06tw026l5377tptk2zswrc645")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9ng4zh0gm3r2j70tffgq7k5xkzr4w5wa08qvje2mslxf6gaqayk2")
            .poolHash("pool1gclysx2h7fndj0jdajlmwvqr8q9tzu3rurjknacu0ff954fsg9a")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9p26xk0cnjccs2an3qsm6fqca3uqp3e94l6dxu6pxvxkyqxa7q82")
            .poolHash("pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99gkx28kt8n4nwygw5fqg4y2d5rc7pmlpj03vr6t8v5xecqwdemh")
            .poolHash("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch217(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxtz2pr9zchzvhkzs9qee0fhdm2jha45stuezqgwdxxj4nsywslda")
            .poolHash("pool15yyxtkhz64p7a8cnax9l7u82s9t9hdhyxsa3tdm977qhgpnsuhq")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxnjclll73p94nvu7r69l5ykv5gvm0gprkkjdp428a7sf0cxf5k6e")
            .poolHash("pool1dxkf7k3qwnpgpxqfye8y9jvk7lver3fk8dq7c4dpzezyyx8cf5c")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u94uryf7q43pvq8ntvysx9l5kzqpgcluudw4dcrr6gngxnq62qv74")
            .poolHash("pool1aqg6fvhcaulvss2ruvpx6ur9vj7pejvdcxv6xp0qlwuwx94evf0")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9rhwql753l8u3086wnlnw66dndsyrtk74egmqemz8ymk5gpxdzeu")
            .poolHash("pool136wr0g23dw9daekuyqtgk366vywms6meespgpm3hdguxqczaaug")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99gkx28kt8n4nwygw5fqg4y2d5rc7pmlpj03vr6t8v5xecqwdemh")
            .poolHash("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch218(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8842x9ekgaf06xyekkne846567cvza0r8u0xykskspechgg4r9zr")
            .poolHash("pool1gclysx2h7fndj0jdajlmwvqr8q9tzu3rurjknacu0ff954fsg9a")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99gkx28kt8n4nwygw5fqg4y2d5rc7pmlpj03vr6t8v5xecqwdemh")
            .poolHash("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch219(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxkmdzry75vn45xglzc8kvqzas64829qm3py4u4znnlvnvgauvm4h")
            .poolHash("pool1qqqyv9pn9typyqwcxqk5ewpxy5p27g5j2ms58hpp2c2kuzs5z77")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99gkx28kt8n4nwygw5fqg4y2d5rc7pmlpj03vr6t8v5xecqwdemh")
            .poolHash("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch220(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxjmepf6mf0j3ufvdla96c9vu8gqfu2qk5v4hz5rz6hvvlsqsjwy6")
            .poolHash("pool1hntu7agmt8u5j9c20ejen7dvq0jfkvkpnul3mrdd8tppqvwfvt2")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uy58u98406966ckjt06zmr7ehfyerr6uqccy45cr8342t8cqq40nx")
            .poolHash("pool1cduc7ut6grhqyavrmm3gzcdf4qfdsjk7vdwcxy9c0r5fyaxh2sq")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch221(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool19066qvd5dv6vq7fh5a5l7muzk6nc5fw8zq3w4tclyrhvjvlyeuc")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u85egvvcnr0nm3rskyua8jv630t59xldmsg0jw9nm3kt3csvawqv8")
            .poolHash("pool1ljlmfg7p37ysmea9ra5xqwccue203dpj40w6zlzn5r2cvjrf6tw")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u82jgggp0sxrkjmvns6gfsrptc3457xx2ppv4gsr9jawwzgpk9afx")
            .poolHash("pool1z7n2ruhmxmv77f6cqhd3wsy6774h2wuay77agxuf2y9mj8q55vw")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxndcr4g08w80hefhacuuw854vwvqvtfgevl9xtgs4u72wchyyfs3")
            .poolHash("pool1qqqqpanw9zc0rzh0yp247nzf2s35uvnsm7aaesfl2nnejaev0uc")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uyvu34wrlst772jsu5ftlly5mdk2tc95atrggl24flj7tngpmqsmp")
            .poolHash("pool1qqqqpanw9zc0rzh0yp247nzf2s35uvnsm7aaesfl2nnejaev0uc")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch222(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8ra0dcr53je95vdhmm0q0lfgjys428fhy4w4dp6lelfavg7lmae7")
            .poolHash("pool12vs4c3cm0tr49c7alrevfs0xa5g3s4al4fn46h33e69uusat04v")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45")
            .rewardType(RewardType.LEADER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch223(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8g2g367tpazed2mgu8aaewdszfd3rerxv5gxlh9uuwj9qqaklxem")
            .poolHash("pool1myvgx4ef424e6nw2strqq7dxcepav52plyyhxrk0avaw7y098l2")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch224(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8pzuph8xp8qqgh2tjp5ngm5gvgrhg784twmyj0cv2hkfsg0upea2")
            .poolHash("pool1hht4pctn70tqdsd0zwuxv8d8cz6l5scjzaquty7vamyzy78eqvc")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch225(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxcc3whk9uwt8rpp4tgjt0hj9l3u0mrn2h52n747qlwayyc23crfu")
            .poolHash("pool12nz4esgsex6zdq9q0208gw9zkdrt3f42rrkefzaxqaqzzlwjku7")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9e4uepavzls0xemezld9rqjqj9nef70ytcpt8ppl0fjatqvlam8p")
            .poolHash("pool1qqqqrwzy7njvjq9wph7usj5gghm3py9c97688ek8pgc7uem9cfz")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9e4uepavzls0xemezld9rqjqj9nef70ytcpt8ppl0fjatqvlam8p")
            .poolHash("pool1qqqqrwzy7njvjq9wph7usj5gghm3py9c97688ek8pgc7uem9cfz")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxdmmmx8jfhx2jeyn8qpd4zdge8nva97mkaze3nru6wkhngp232j0")
            .poolHash("pool1vquhv3kh6xkklckaenl8alyuhfsld57ef5573aqauueyqacx4ak")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxly7krmu7v5taf6yzmqmamtwp673t5m0j5ed4a3rcjty9cgwdrh4")
            .poolHash("pool1qqqyv9pn9typyqwcxqk5ewpxy5p27g5j2ms58hpp2c2kuzs5z77")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uyepuk9gcscsnjmtnwl0u097p88zrgvt44t23wtucql22pgwvaznw")
            .poolHash("pool1ddgcpgjcyawxw95se9x8qnc8gxgwjr4fqrk4vk6v9x47s0ggpjp")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uyse6jl27hfm3kw92yus7wmtr62gdjxy55fpdnz7ydn6m9qhnk4qn")
            .poolHash("pool1vx9tzlkgafernd9vpjpxkenutx2gncj4yn88fpq69823qlwcqrt")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8wyeqvkw5m6ef8tjnqxyv8p9mhnc4z7lvh2cm46748jmtgtrr6p0")
            .poolHash("pool1v4xevkf7fx08pzy7atsk59dds6p0sdyl6xmhwf4mh79x6l5p6kx")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9n5hhru384pmg86a4y92qlmdnlrupe8wyu8tsrhpxgc89q6q0ed3")
            .poolHash("pool1e556526sqwugxwnmeumt9lhj5jukklg8vv3ynk75xt9vs7adr5y")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch226(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99gkx28kt8n4nwygw5fqg4y2d5rc7pmlpj03vr6t8v5xecqwdemh")
            .poolHash("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool150n38x8gquu4yt5s0s6m3ajjqe89y68x96dh32tq8a4es2l6dvp")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9580h9cv6zck2jfgy63exxdvuszyc4lmq4qzm9m4grnjfcprdah4")
            .poolHash("pool1xt7mjrtnsew3v33lu8sf93upf20sxhmcrfnpm82ra46yxk7uy45")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8d3pdradkydnm4k8x9a7h9erzxt5au72td5nukrw5m5ans75yg78")
            .poolHash("pool1cduc7ut6grhqyavrmm3gzcdf4qfdsjk7vdwcxy9c0r5fyaxh2sq")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch227(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9x2llh3f8a9snrdhwmguwncqnsczccx73jm2mty2cc5f6qm7ucz9")
            .poolHash("pool1jg3fmnmc9n52sgzslhhvhye5e3xeqmrwkekdhh82sma47r47zg0")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxjrcf86k4yuczp2r7hw8uy30raur6863a6eprpjr3juz6clgute2")
            .poolHash("pool1v4xevkf7fx08pzy7atsk59dds6p0sdyl6xmhwf4mh79x6l5p6kx")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u97h59ht8c565e4yv6e7tztwr8clax935n6w2rqfzts0rgqdce73x")
            .poolHash("pool1qqqqqdk4zhsjuxxd8jyvwncf5eucfskz0xjjj64fdmlgj735lr9")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch228(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uydzd8jgucw9zzy2xhpk9e7ra27m6xzqa6cyghcu8dkf09qs4v2sl")
            .poolHash("pool12mmcc4rc2fzfv7gyv8h06nvnsrm3m7erzdv8x6gzvjxlu2lf09n")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u99jtwe0zgq4yq9ehexqpvpu4j0hgs5sdwy7u85acz280ncvkv7cw")
            .poolHash("pool1nudasuv0lljpe0tswcverqvlh0v4226vgnpsuxn0e9nkvnvcvne")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9pnva6rkcmlt4hem202v2w65zn2ggsn9reuzlwczvwu05q2lcngu")
            .poolHash("pool1ell3xjtspzzz4vtsatscan6rheltf7j3hh2s8qsam2h3jvcxzm9")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch229(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9wra8z90zj9anj79vzn99sd90jlynwnjkkmtjg6nvx9xjs7mzup4")
            .poolHash("pool14skj6e4rpjanzclx3fc880xnl8xafgg63tmw93t9xspvwx985qu")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u89yfe2048cyf9f2cs0f7gswerlh4s32gmnw6r97lw7pyvqt85nmt")
            .poolHash("pool1vxz0deezj5c2950e7arpzfqxzq8zd9kawsullrzjw5rsq0yhxgr")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uyy2whe25ts5qzllulgk4dfsjr9qzks8dz2uzfcgz5j5mhqh5rzjh")
            .poolHash("pool1vx9tzlkgafernd9vpjpxkenutx2gncj4yn88fpq69823qlwcqrt")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8zg7h8tvpjclndj5l4zuhtjvpd833mu5c8n39q9gr89knsl5j6qz")
            .poolHash("pool1qvhss6kxmtl27nr4ugvypayhvt4a4vzll8p9csdg2v8q7d22llu")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch230(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8qj0ax56ntjnvaghgffwexrvmr6jmyepqz5d4ql8vv22xc90092t")
            .poolHash("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxrkez465cmpdrragd3rhcgrc6lllnasxd7qtl735l489egx9yfxe")
            .poolHash("pool19w5khsnmu27au0kprw0kjm8jr7knneysj7lfkqvnu66hyz0jxsx")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uygyqumaaaxrdq5z0p8q9tw8tsz7760ascknheqrs2ylx2qgmgrk7")
            .poolHash("pool1yxhf37v04q6cyrf8a4g64xsmvepwg5t2zc0qcquqjlph55ye2ef")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8jv4f0823el83azk528uqcu8f5kdjxvfpw7fvtf6nvuyjs794w6u")
            .poolHash("pool1rvng7n968748udkc5rxy4h9zp9hms4s3jsfwuues76ft28uc056")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8xq7a8jyknqudx7dcph3ldvqpt3ecnwc7a2vl3te6397cc2m38z4")
            .poolHash("pool14skj6e4rpjanzclx3fc880xnl8xafgg63tmw93t9xspvwx985qu")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uy5kcpd3ja0hx3ud0xd3s6jr5e2dvkyfhec0z4xm6vg6sssx520vg")
            .poolHash("pool1m62sl6rauje9cknrkhwl39tc4hujudkd7gp478dpz7tagmjr8wm")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u9d5af7g6zm3j23f0j37my24xrdfl95lv6eft9sycq4n88cy9ujvf")
            .poolHash("pool18v9r8afalh50l4lstct2awdc3zspnvurcs7t45nv29uc2mnxc6c")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uyga7tyr8dtsa6fg7a9ncq692330j8m44gmz4jn82p07ueqd67szv")
            .poolHash("pool1pnzwgsgzd6t4788sfr7dxjyusepyq9xaxnpyfcngewqf29t9ayd")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u85w6uns0rk899p78sa3wu3yd3t97ku5pjyvsths0cn7sxqyft0f9")
            .poolHash("pool12mmcc4rc2fzfv7gyv8h06nvnsrm3m7erzdv8x6gzvjxlu2lf09n")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8r60srves2r3evyyumrju3r8y8r0ccd96xgulwcdul7s4gerc8mf")
            .poolHash("pool1lkpj6fa4l0sy39qfvyl4pcsnnd22y8np8j98vzwula2kuxgers4")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }

  public Collection<Reward> removeRewardAddressDidNotReceiveInEpoch231(Collection<Reward> rewards) {
    List<MissingReward> missingRewards = new ArrayList<>();
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uxrkez465cmpdrragd3rhcgrc6lllnasxd7qtl735l489egx9yfxe")
            .poolHash("pool1qqqqqdk4zhsjuxxd8jyvwncf5eucfskz0xjjj64fdmlgj735lr9")
            .rewardType(RewardType.LEADER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u89yfe2048cyf9f2cs0f7gswerlh4s32gmnw6r97lw7pyvqt85nmt")
            .poolHash("pool1vxz0deezj5c2950e7arpzfqxzq8zd9kawsullrzjw5rsq0yhxgr")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1uyy2whe25ts5qzllulgk4dfsjr9qzks8dz2uzfcgz5j5mhqh5rzjh")
            .poolHash("pool1vx9tzlkgafernd9vpjpxkenutx2gncj4yn88fpq69823qlwcqrt")
            .rewardType(RewardType.MEMBER)
            .build());
    missingRewards.add(
        MissingReward.builder()
            .stakeAddress("stake1u8zg7h8tvpjclndj5l4zuhtjvpd833mu5c8n39q9gr89knsl5j6qz")
            .poolHash("pool1qvhss6kxmtl27nr4ugvypayhvt4a4vzll8p9csdg2v8q7d22llu")
            .rewardType(RewardType.MEMBER)
            .build());
    return modifyRewards(rewards, missingRewards);
  }
}
