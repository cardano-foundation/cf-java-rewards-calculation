package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.DataProviderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.cardanofoundation.rewards.util.CurrencyConverter.adaToLovelace;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class PoolRewardCalculationTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;

    @Autowired
    JsonDataProvider jsonDataProvider;


    private void Test_calculatePoolReward(final String poolId,
                                                   final int epoch,
                                                   final DataProviderType dataProviderType) {

        DataProvider dataProvider;
        if (dataProviderType == DataProviderType.KOIOS) {
            dataProvider = koiosDataProvider;
        } else if (dataProviderType == DataProviderType.JSON) {
            dataProvider = jsonDataProvider;
        } else {
            throw new RuntimeException("Unknown data provider type: " + dataProviderType);
        }

       PoolRewardCalculationResult poolRewardCalculationResult =
                PoolRewardCalculation.calculatePoolRewardInEpoch(poolId, epoch, dataProvider);

       PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch);
       if (poolHistoryCurrentEpoch == null) {
           Assertions.assertEquals(0.0, poolRewardCalculationResult.getPoolReward());
           return;
       }

       double difference = (poolHistoryCurrentEpoch.getDelegatorRewards() - (poolRewardCalculationResult.getPoolReward() - poolRewardCalculationResult.getPoolFee()));

       Assertions.assertTrue(Math.abs(difference) < adaToLovelace(1), "The difference between expected pool reward and actual pool reward is greater than 1 ADA: " + lovelaceToAda(difference) + " ADA");
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
        return IntStream.range(211, 432).boxed();
    }

    @ParameterizedTest
    @MethodSource("testPoolJsonProviderRewardRange")
    void calculateNorthPoolRewardFromEpoch211To432(int epoch) {
        String poolId = "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx";
        Test_calculatePoolReward(poolId, epoch, DataProviderType.JSON);
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
