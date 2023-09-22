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

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;

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

        DataProvider dataProvider = null;
        if (dataProviderType == DataProviderType.KOIOS) {
            dataProvider = koiosDataProvider;
        } else if (dataProviderType == DataProviderType.JSON) {
            dataProvider = jsonDataProvider;
        } else {
            throw new RuntimeException("Unknown data provider type: " + dataProviderType);
        }

        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210
        PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch);
        double poolStake = poolHistoryCurrentEpoch.getActiveStake();
        double expectedPoolReward = poolHistoryCurrentEpoch.getDelegatorRewards();
        double poolFees = poolHistoryCurrentEpoch.getPoolFees();
        int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCount();

        if (blocksPoolHasMinted == 0) {
            Assertions.assertEquals(expectedPoolReward, 0.0);
            return;
        }

        // Step 2: Get Epoch information of current epoch
        // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
        Epoch epochInfo = dataProvider.getEpochInfo(epoch);

        double activeStakeInEpoch = 0;
        if (epochInfo.getActiveStake() != null) {
            activeStakeInEpoch = epochInfo.getActiveStake();
        }

        // The Shelley era and the ada pot system started on mainnet in epoch 208.
        // Fee and treasury values are 0 for epoch 208.
        double totalFeesForCurrentEpoch = 0.0;
        if (epoch > 209) {
            totalFeesForCurrentEpoch = epochInfo.getFees();
        }

        int totalBlocksInEpoch = epochInfo.getBlockCount();

        if (epoch > 212 && epoch < 255) {
            totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
        }

        // Get the ada reserves for the next epoch because it was already updated yet
        AdaPots adaPotsForNextEpoch = dataProvider.getAdaPotsForEpoch(epoch + 1);
        double reserves = adaPotsForNextEpoch.getReserves();

        // Step 3: Get total ada in circulation
        double adaInCirculation = TOTAL_LOVELACE - reserves;

        // Step 4: Get protocol parameters for current epoch
        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch);
        double decentralizationParameter = protocolParameters.getDecentralisation();
        int optimalPoolCount = protocolParameters.getOptimalPoolCount();
        double influenceParam = protocolParameters.getPoolOwnerInfluence();
        double monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
        double treasuryGrowRate = protocolParameters.getTreasuryGrowRate();

        // Step 5: Calculate apparent pool performance
        var apparentPoolPerformance =
                PoolRewardCalculation.calculateApparentPoolPerformance(poolStake, activeStakeInEpoch,
                        blocksPoolHasMinted, totalBlocksInEpoch, decentralizationParameter);

        // Step 6: Calculate total available reward for pools (total reward pot after treasury cut)
        // -----
        double totalRewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserves, totalFeesForCurrentEpoch);

        double stakePoolRewardsPot = totalRewardPot - Math.floor(totalRewardPot * treasuryGrowRate);

        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"

        // Step 7: Get the latest pool update before this epoch and extract the pledge
        double poolOwnerActiveStake = dataProvider.getPoolPledgeInEpoch(poolId, epoch);

        PoolOwnerHistory poolOwnersHistoryInEpoch = dataProvider.getHistoryOfPoolOwnersInEpoch(poolId, epoch);
        if (poolOwnersHistoryInEpoch.getActiveStake() < poolOwnerActiveStake) {
            Assertions.assertEquals(expectedPoolReward, 0.0);
            return;
        }

        double relativeStakeOfPoolOwner = poolOwnerActiveStake / adaInCirculation;
        double relativePoolStake = poolStake / adaInCirculation;

        // Step 8: Calculate optimal pool reward
        double optimalPoolReward =
                PoolRewardCalculation.calculateOptimalPoolReward(
                        stakePoolRewardsPot,
                        optimalPoolCount,
                        influenceParam,
                        relativePoolStake,
                        relativeStakeOfPoolOwner);

        // Step 9: Calculate pool reward as optimal pool reward * apparent pool performance
        double poolReward = PoolRewardCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);

        // Step 10: Compare estimated pool reward with actual pool reward minus pool fees
        System.out.println("Difference between expected pool reward and actual pool reward: " +
                (expectedPoolReward - (poolReward - poolFees)));
        Assertions.assertEquals(Math.round(expectedPoolReward),
                Math.round(poolReward - poolFees));

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
    void calculatePoolRewardInEpoch211(String poolId) {
        int epoch = 213;
        Test_calculatePoolReward(poolId, epoch, DataProviderType.JSON);
    }

    static Stream<Integer> testPoolRewardRange() {
        return IntStream.range(211, 433).boxed();
    }

    @ParameterizedTest
    @MethodSource("testPoolRewardRange")
    void calculateNorthPoolRewardFromEpoch211To216(int epoch) {
        String poolId = "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx";
        Test_calculatePoolReward(poolId, epoch, DataProviderType.JSON);
    }

    @ParameterizedTest
    @MethodSource("testPoolRewardRange")
    void calculateOCTASPoolRewardFromEpoch211To216(int epoch) {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        Test_calculatePoolReward(poolId, epoch, DataProviderType.KOIOS);

    }
}
