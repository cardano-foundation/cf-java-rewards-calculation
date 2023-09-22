package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import rest.koios.client.backend.api.base.exception.ApiException;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class PoolRewardCalculationTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;


    PoolCalculationResult Test_calculatePoolReward(final String poolId,
                                                   final int epoch,
                                                   final boolean skipTest,
                                                   final Double totalRewardPotOverride) throws ApiException {
        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210
        PoolHistory poolHistoryCurrentEpoch = koiosDataProvider.getPoolHistory(poolId, epoch);
        double poolStake = poolHistoryCurrentEpoch.getActiveStake();
        double expectedPoolReward = poolHistoryCurrentEpoch.getDelegatorRewards();
        double poolFees = poolHistoryCurrentEpoch.getPoolFees();
        int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCount();

        // Step 2: Get Epoch information of current epoch
        // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
        Epoch epochInfo = koiosDataProvider.getEpochInfo(epoch);

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
        AdaPots adaPotsForNextEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch + 1);
        double reserves = adaPotsForNextEpoch.getReserves();

        // Step 3: Get total ada in circulation
        double adaInCirculation = adaPotsForNextEpoch.getAdaInCirculation();

        // Step 4: Get protocol parameters for current epoch
        ProtocolParameters protocolParameters = koiosDataProvider.getProtocolParametersForEpoch(epoch);
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

        if (totalRewardPotOverride != null) {
            totalRewardPot = totalRewardPotOverride;
        }

        double stakePoolRewardsPot = totalRewardPot - Math.floor(totalRewardPot * treasuryGrowRate);

        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"

        // Step 7: Get the latest pool update before this epoch and extract the pledge
        double poolOwnerActiveStake = koiosDataProvider.getPoolPledgeInEpoch(poolId, epoch);

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
        if (!skipTest) {
            System.out.println("Difference between expected pool reward and actual pool reward: " +
                    (expectedPoolReward - (poolReward - poolFees)));
            Assertions.assertEquals(Math.round(expectedPoolReward),
                    Math.round(poolReward - poolFees));
        }

        return PoolCalculationResult.builder()
                .actualPoolReward(poolReward - poolFees)
                .expectedPoolReward(expectedPoolReward)
                .stakePoolRewardsPot(stakePoolRewardsPot)
                .totalRewardPot(totalRewardPot)
                .influenceParam(influenceParam)
                .optimalPoolCount(optimalPoolCount)
                .relativeStakeOfPool(relativePoolStake)
                .relativeStakeOfPoolOwner(relativeStakeOfPoolOwner)
                .poolPerformance(apparentPoolPerformance)
                .poolFee(poolFees)
                .build();
    }

    void Test_calculatePoolReward(String poolId, int epoch) throws ApiException {
        Test_calculatePoolReward(poolId, epoch, false, null);
    }

    void Test_calculatePoolReward(String poolId, int epoch, Double totalRewardPotOverride) throws ApiException {
        Test_calculatePoolReward(poolId, epoch, false, totalRewardPotOverride);
    }

    static Stream<String> testPoolIds() {
        return Stream.of(
                "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9",
                "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt",
                "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx"
        );
    }

    @ParameterizedTest
    @MethodSource("testPoolIds")
    void calculatePoolRewardInEpoch211(String poolId) throws ApiException {
        int epoch = 211;
        Test_calculatePoolReward(poolId, epoch);
    }

    static Stream<Integer> testPoolRewardRange() {
        return IntStream.range(212, 216).boxed();
    }

    @ParameterizedTest
    @MethodSource("testPoolRewardRange")
    void calculateSwimPoolRewardFromEpoch211To216(int epoch) throws ApiException {
        String poolId = "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9";
        Test_calculatePoolReward(poolId, epoch);
    }

    @ParameterizedTest
    @MethodSource("testPoolRewardRange")
    void calculateOCTASPoolRewardFromEpoch211To216(int epoch) throws ApiException {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        Test_calculatePoolReward(poolId, epoch);

    }

    // TODO: Works with: totalRewardPot = new BigDecimal("38941407");
    @Test
    void calculateSWIMPoolRewardFromEpoch215() throws ApiException {
        String poolId = "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9";
        Test_calculatePoolReward(poolId, 215);
    }

    // TODO: Works with: totalRewardPot = new BigDecimal("38941375");
    @Test
    void calculateOCTASPoolRewardFromEpoch215() throws ApiException {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        Test_calculatePoolReward(poolId, 215);
    }

    /*
     * The idea with this test is to figure out if the pool rewards formula itself is correct, if the total reward pot
     * is correct. Therefore, we calculate the total reward pot with the reverse formula and for one pool and cross-check
     * this ideal reward pot with a different pool.
     */
    @ParameterizedTest
    @MethodSource("testPoolRewardRange")
    void figureOutATotalRewardPot(final int epoch) throws ApiException {
        final String poolId = "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9";
        PoolCalculationResult poolCalculationResult = Test_calculatePoolReward(poolId, epoch, true, null);

        final double totalStakePoolRewardPot = PoolRewardCalculation.calculateRewardPotByOptimalPoolReward(
                poolCalculationResult.getActualPoolRewardWithFee(),
                poolCalculationResult.getOptimalPoolCount(),
                poolCalculationResult.getInfluenceParam(),
                poolCalculationResult.getRelativeStakeOfPool(),
                poolCalculationResult.getRelativeStakeOfPoolOwner(),
                poolCalculationResult.getPoolPerformance()
        );

        double totalRewardPot = totalStakePoolRewardPot / 0.8;
        double totalRewardPotInAda = Math.floor(totalRewardPot);

        //Assertions.assertEquals(totalRewardPotInAda,
        //        poolCalculationResult.getTotalRewardPot().setScale(0, mathContext.getRoundingMode()));

        totalRewardPot = PoolRewardCalculation.calculateRewardPotByOptimalPoolReward(
                poolCalculationResult.getExpectedPoolReward() + poolCalculationResult.getPoolFee(),
                poolCalculationResult.getOptimalPoolCount(),
                poolCalculationResult.getInfluenceParam(),
                poolCalculationResult.getRelativeStakeOfPool(),
                poolCalculationResult.getRelativeStakeOfPoolOwner(),
                poolCalculationResult.getPoolPerformance()
        ) / 0.8;
        totalRewardPotInAda = Math.floor(totalRewardPot);

        System.out.println("Calculating total reward pot for epoch: " + epoch);
        System.out.println("The formula gives a total reward pot of: " +
                Math.round(poolCalculationResult.getTotalRewardPot()));
        System.out.println("But the reverse formula with the expected pool reward gives a total reward pot of: " +
                totalRewardPotInAda);
        System.out.println("The difference is: " +
                (Math.round(poolCalculationResult.getTotalRewardPot()) - totalRewardPotInAda));

        String poolId2 = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        poolCalculationResult = Test_calculatePoolReward(poolId2, epoch, true, totalRewardPotInAda);
        double difference = Math.round(poolCalculationResult.getActualPoolReward()) -
                Math.round(poolCalculationResult.getExpectedPoolReward());

        System.out.println("Difference between expected pool reward and actual pool reward: " + Math.abs(difference));
        assert(Math.abs(difference) < 25.0);
    }
}
