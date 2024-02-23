package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.domain.PoolRewardCalculationResult;
import org.cardanofoundation.rewards.calculation.domain.Reward;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.validation.enums.DataProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.DisabledIf;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.isHigher;
import static org.cardanofoundation.rewards.calculation.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class PoolRewardComputationTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;

    @Autowired
    JsonDataProvider jsonDataProvider;

    @Autowired(required = false)
    DbSyncDataProvider dbSyncDataProvider;

    private void Test_calculatePoolReward(final String poolId,
                                                   final int epoch,
                                                   final DataProviderType dataProviderType) {
        DataProvider dataProvider;
        if (dataProviderType == DataProviderType.KOIOS) {
            dataProvider = koiosDataProvider;
        } else if (dataProviderType == DataProviderType.JSON) {
            dataProvider = jsonDataProvider;
        } else if (dataProviderType == DataProviderType.DB_SYNC) {
            dataProvider = dbSyncDataProvider;
        } else {
            throw new RuntimeException("Unknown data provider type: " + dataProviderType);
        }

       PoolRewardCalculationResult poolRewardCalculationResult =
                PoolRewardComputation.computePoolRewardInEpoch(poolId, epoch, dataProvider);

       List<Reward> memberRewardsInEpoch = dataProvider.getMemberRewardsInEpoch(epoch);
       List<Reward> actualPoolRewardsInEpoch = memberRewardsInEpoch.stream()
               .filter(reward -> reward.getPoolId().equals(poolId))
               .toList();
       if (actualPoolRewardsInEpoch.isEmpty()) {
           Assertions.assertEquals(BigInteger.ZERO, poolRewardCalculationResult.getPoolReward());
           return;
       }

       BigInteger totalDifference = BigInteger.ZERO;
       int rewardIndex = 0;
       for (Reward reward : actualPoolRewardsInEpoch) {
           Reward memberReward = poolRewardCalculationResult.getMemberRewards().stream()
                   .filter(member -> member.getStakeAddress().equals(reward.getStakeAddress()))
                   .findFirst()
                   .orElse(null);
           Assertions.assertNotNull(memberReward, "Member reward not found for stake address: " + reward.getStakeAddress());
           BigInteger difference = reward.getAmount().subtract(memberReward.getAmount()).abs();

           if (poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(reward.getStakeAddress())) {
               BigInteger poolOwnerReward = poolRewardCalculationResult.getOperatorReward();
               difference = reward.getAmount().subtract(poolOwnerReward).abs();
           }
           totalDifference = totalDifference.add(difference);

           System.out.println("[" + rewardIndex + "] The difference between expected member " + reward.getStakeAddress() + " reward and actual member reward is : " + lovelaceToAda(difference.intValue()) + " ADA");
           rewardIndex++;
           //Assertions.assertEquals(0.0, difference, "The difference between expected member reward and actual member reward is : " + lovelaceToAda(difference) + " ADA");
       }

       System.out.println("Total difference: " + lovelaceToAda(totalDifference.intValue()) + " ADA");

       BigInteger totalNoReward = BigInteger.ZERO;
       BigInteger coOwnerReward = BigInteger.ZERO;
       BigInteger calculatedMemberRewards = BigInteger.ZERO;
       if (poolRewardCalculationResult.getMemberRewards() != null) {
           for (Reward memberReward : poolRewardCalculationResult.getMemberRewards()) {
               Reward actualReward = actualPoolRewardsInEpoch.stream()
                       .filter(reward -> reward.getStakeAddress().equals(memberReward.getStakeAddress()))
                       .findFirst()
                       .orElse(null);
               if (actualReward == null && isHigher(memberReward.getAmount(), BigInteger.ZERO) && !poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                   totalNoReward = totalNoReward.add(memberReward.getAmount());
                   System.out.println("No reward! The difference between expected member " + memberReward.getStakeAddress() + " reward and actual member reward is : " + lovelaceToAda(memberReward.getAmount().intValue()) + " ADA");
               }

               // Co-owner reward is not included in the member rewards and would be added to the reward address of the pool
               if(poolRewardCalculationResult.getPoolOwnerStakeAddresses().contains(memberReward.getStakeAddress())) {
                   coOwnerReward = coOwnerReward.add(memberReward.getAmount());
               }
           }
           calculatedMemberRewards = poolRewardCalculationResult.getMemberRewards().stream().map(Reward::getAmount).reduce(BigInteger.ZERO, BigInteger::add);
       }
       System.out.println("Total no reward: " + lovelaceToAda(totalNoReward.intValue()) + " ADA");

       BigInteger totalActualPoolRewards = dataProvider.getTotalPoolRewardsInEpoch(poolId, epoch);

       if (totalActualPoolRewards == null) {
           totalActualPoolRewards = BigInteger.ZERO;
       }

       BigInteger operatorReward = poolRewardCalculationResult.getOperatorReward();

       if (operatorReward == null) {
           operatorReward = BigInteger.ZERO;
       }

       BigInteger difference = totalActualPoolRewards.subtract(calculatedMemberRewards.subtract(coOwnerReward).add(operatorReward));

       Assertions.assertEquals(BigInteger.ZERO, difference, "The difference between expected pool reward and actual pool reward is : " + lovelaceToAda(difference.intValue()) + " ADA");
    }

    static Stream<String> testPoolIds() {
        return Stream.of(
                "pool1qqqqx69ztfvd83rtafs3mk4lwanehrglmp2vwkjpaguecs2t4c2",
                "pool19ctjr5ft75sz396hn0tf6ns4hy5w9l9jp2jh3m8mx6acvm2cn7j",
                "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9",
                "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt",
                "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx",
                "pool1spus7k8cy5qcs82xhw60dwwk2d4vrfs0m5vr2zst04gtq700gjn",
                "pool13n4jzw847sspllczxgnza7vkq80m8px7mpvwnsqthyy2790vmyc",
                "pool1ljlmfg7p37ysmea9ra5xqwccue203dpj40w6zlzn5r2cvjrf6tw"
        );
    }

    @ParameterizedTest
    @MethodSource("testPoolIds")
    void calculatePoolRewardInEpoch213(String poolId) {
        int epoch = 213;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.JSON);
    }

    static Stream<Integer> testPoolJsonProviderRewardRange() {
        return IntStream.range(211, 220).boxed();
    }

    @ParameterizedTest
    @MethodSource("testPoolJsonProviderRewardRange")
    @DisabledIf(expression = "#{environment.acceptsProfiles('ci')}", loadContext = true, reason = "Range test is too long for CI")
    void calculateNorthPoolRewardFromEpoch211To216(int epoch) {
        String poolId = "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx";
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateNorthPoolRewardInEpoch217() {
        String poolId = "pool1rcd7qslz3xnpnk9u6tcwegy5r2574uf8suqu24ptg637jha3sr5";
        int epoch = 217;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateSTKH1PoolRewardInEpoch363() {
        String poolId = "pool1kchver88u3kygsak8wgll7htr8uxn5v35lfrsyy842nkscrzyvj";
        int epoch = 363; // TODO: handle no rewards in 350
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateAutoStakeIIPoolRewardInEpoch215() {
        String poolId = "pool1ljywsch33t7gaf02aeeqkqpuku0ngpwl9fm04t7u5sl5xyunmgp";
        int epoch = 215;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }
    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateXYZPoolRewardInEpoch213() {
        String poolId = "pool18hkq2t8ss45h4fkr92f52flhc4mpzedx5mcz4xhnpj0dzp76028";
        int epoch = 213;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateXYZPoolRewardInEpoch211() {
        String poolId = "pool18hkq2t8ss45h4fkr92f52flhc4mpzedx5mcz4xhnpj0dzp76028";
        int epoch = 211;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateUNDRPoolRewardInEpoch211() {
        String poolId = "pool140x77a7cz2j689lmxf836qpjtc3y83rka28v6gpswxdw2mag62e";
        int epoch = 211;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateJP777PoolRewardInEpoch211() {
        String poolId = "pool1x5jlxxqce4gkr4474q0gcsmu47wfqjt9mksv2w38ujpzgjn83ye";
        int epoch = 211;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateJAPANPoolRewardInEpoch211() {
        String poolId = "pool19uynx6nxcdksmaqdcshjg487fap3rs3axyhrdqa7gdqgzgxss4y";
        int epoch = 211;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateXYZPoolRewardInEpoch212() {
        String poolId = "pool18hkq2t8ss45h4fkr92f52flhc4mpzedx5mcz4xhnpj0dzp76028";
        int epoch = 212;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateAAA2PoolRewardInEpoch212() {
        String poolId = "pool1a3u8zuz7fqavxgl48s8k37w8w6njk7zevlzyfjdlzxfexjcue6a";
        int epoch = 212;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateDUCKPoolRewardInEpoch212() {
        String poolId = "pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m";
        int epoch = 212;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateViperPoolRewardInEpoch213() {
        String poolId = "pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr";
        int epoch = 213;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    @Test
    @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
    void calculateJazzPoolRewardInEpoch214() {
        String poolId = "pool1h0524mtazrjnzqh5e4u060jsfk8lpsqqjfpa5gygjwuhqu34wvt";
        int epoch = 214;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.DB_SYNC);
    }

    static Stream<Integer> testPoolKoiosProviderRewardRange() {
        return IntStream.range(211, 213).boxed();
    }

    @ParameterizedTest
    @MethodSource("testPoolKoiosProviderRewardRange")
    void calculateOCTASPoolRewardFromEpoch211To213(int epoch) {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        Test_calculatePoolReward(poolId, epoch, DataProviderType.KOIOS);
    }
}
