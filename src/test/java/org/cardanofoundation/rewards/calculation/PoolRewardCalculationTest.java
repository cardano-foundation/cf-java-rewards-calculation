package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.entity.PoolCalculationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.epoch.model.EpochInfo;
import rest.koios.client.backend.api.epoch.model.EpochParams;
import rest.koios.client.backend.api.network.model.Totals;
import rest.koios.client.backend.api.pool.model.PoolHistory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.CurrencyConverter.adaToLovelace;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class PoolRewardCalculationTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;

    PoolCalculationResult Test_calculatePoolReward(String poolId, int epoch, boolean skipTest, BigDecimal totalRewardPotOverride) throws ApiException {
        MathContext mathContext = new MathContext(30, RoundingMode.HALF_UP);
        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210
        PoolHistory poolHistoryCurrentEpoch = koiosDataProvider.getPoolHistory(poolId, epoch);
        BigDecimal poolStake = lovelaceToAda(new BigDecimal(poolHistoryCurrentEpoch.getActiveStake()));
        BigDecimal expectedPoolReward = lovelaceToAda(new BigDecimal(poolHistoryCurrentEpoch.getDelegRewards()));
        BigDecimal poolFees = lovelaceToAda(new BigDecimal(poolHistoryCurrentEpoch.getPoolFees()));
        int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCnt();

        // Step 2: Get Epoch information of current epoch
        // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
        EpochInfo epochInfo = koiosDataProvider.getEpochInfo(epoch);
        int totalBlocksInEpoch = epochInfo.getBlkCount();
        BigDecimal activeStakeInEpoch = lovelaceToAda(new BigDecimal(epochInfo.getActiveStake()));

        // Step 3 Get fee from current and reserve from next epoch
        BigDecimal totalFeesInEpoch = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(epoch));

        Totals adaPotsForNextEpoch = koiosDataProvider.getAdaPotsForEpoch(epoch + 1);
        BigDecimal reserves = new BigDecimal(adaPotsForNextEpoch.getReserves());

        // Step 4: Get total ada in circulation (total lovelace - reserves)
        BigDecimal adaInCirculation = lovelaceToAda(TOTAL_LOVELACE.subtract(reserves));

        // Step 5: Get protocol parameters for current epoch
        EpochParams epochParams = koiosDataProvider.getProtocolParametersForEpoch(epoch);
        double decentralizationParam = epochParams.getDecentralisation().doubleValue();
        int optimalPoolCount = epochParams.getOptimalPoolCount();
        double influenceParam = epochParams.getInfluence().doubleValue();
        double monetaryExpandRate = epochParams.getMonetaryExpandRate().doubleValue();
        double treasuryGrowRate = epochParams.getTreasuryGrowthRate().doubleValue();

        // Step 6: Calculate apparent pool performance
        var apparentPoolPerformance =
                PoolRewardCalculation.calculateApparentPoolPerformance(poolStake, activeStakeInEpoch,
                        blocksPoolHasMinted, totalBlocksInEpoch, decentralizationParam);

        // Step 7: Calculate total available reward for pools (total reward pot after treasury cut)
        BigDecimal totalRewardPot = lovelaceToAda(TreasuryCalculation.calculateTotalRewardPot(monetaryExpandRate, reserves, totalFeesInEpoch));

        if (totalRewardPotOverride != null) {
            totalRewardPot = totalRewardPotOverride;
        }

        BigDecimal stakePoolRewardsPot = totalRewardPot.multiply(new BigDecimal(1 - treasuryGrowRate));

        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"

        // Step 8: Get the latest pool update before this epoch and extract the pledge
        BigDecimal poolOwnerActiveStake = lovelaceToAda(new BigDecimal(
                koiosDataProvider.getLatestPoolUpdateBeforeOrInEpoch(poolId, epoch - 1).getPledge()));

        BigDecimal relativeStakeOfPoolOwner = poolOwnerActiveStake.divide(adaInCirculation, mathContext);
        BigDecimal relativePoolStake = poolStake.divide(adaInCirculation, mathContext);

        // Step 9: Calculate optimal pool reward
        BigDecimal optimalPoolReward =
                PoolRewardCalculation.calculateOptimalPoolReward(
                        stakePoolRewardsPot,
                        optimalPoolCount,
                        influenceParam,
                        relativePoolStake,
                        relativeStakeOfPoolOwner);

        // Step 10: Calculate pool reward as optimal pool reward * apparent pool performance
        BigDecimal poolReward = PoolRewardCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);

        // Step 11: Compare estimated pool reward with actual pool reward minus pool fees
        if (!skipTest) {
            System.out.println("Difference between expected pool reward and actual pool reward: " +
                    expectedPoolReward.subtract(poolReward.subtract(poolFees)));
            Assertions.assertEquals(expectedPoolReward.setScale(0, mathContext.getRoundingMode()),
                poolReward.subtract(poolFees).setScale(0, mathContext.getRoundingMode()));
        }

        return PoolCalculationResult.builder()
                .actualPoolReward(poolReward.subtract(poolFees))
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

    void Test_calculatePoolReward(String poolId, int epoch, BigDecimal totalRewardPotOverride) throws  ApiException {
        Test_calculatePoolReward(poolId, epoch, false, totalRewardPotOverride);
    }

    @Test
    void calculatePoolRewardInEpoch211() throws ApiException {
        String poolId1 = "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx";
        String poolId2 = "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9";
        String poolId3 = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";

        int epoch = 211;

        Test_calculatePoolReward(poolId1, epoch);
        Test_calculatePoolReward(poolId2, epoch);
        Test_calculatePoolReward(poolId3, epoch);
    }

    @Test
    void calculatePoolRewardInEpoch212() throws ApiException {
        String poolId1 = "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx";
        String poolId2 = "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9";
        String poolId3 = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";

        int epoch = 212;

        Test_calculatePoolReward(poolId1, epoch);
        Test_calculatePoolReward(poolId2, epoch);
        Test_calculatePoolReward(poolId3, epoch);
    }

    static Stream<Integer> testPoolRewardRange() {
        return IntStream.range(213, 250).boxed();
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
    void figureOutATotalRewardPot(int epoch) throws ApiException {
        String poolId = "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9";
        PoolCalculationResult poolCalculationResult = Test_calculatePoolReward(poolId, epoch, true, null);
        MathContext mathContext = new MathContext(30, RoundingMode.HALF_UP);

        BigDecimal totalStakePoolRewardPot = PoolRewardCalculation.calculateRewardPotByOptimalPoolReward(
                poolCalculationResult.getActualPoolRewardWithFee(),
                poolCalculationResult.getOptimalPoolCount(),
                poolCalculationResult.getInfluenceParam(),
                poolCalculationResult.getRelativeStakeOfPool(),
                poolCalculationResult.getRelativeStakeOfPoolOwner(),
                poolCalculationResult.getPoolPerformance()
        );

        BigDecimal totalRewardPot = totalStakePoolRewardPot.divide(new BigDecimal("0.8"), mathContext);
        BigDecimal totalRewardPotInAda = totalRewardPot.setScale(0, mathContext.getRoundingMode());

        //Assertions.assertEquals(totalRewardPotInAda,
        //        poolCalculationResult.getTotalRewardPot().setScale(0, mathContext.getRoundingMode()));

        totalRewardPot = PoolRewardCalculation.calculateRewardPotByOptimalPoolReward(
                poolCalculationResult.getExpectedPoolReward().add(poolCalculationResult.getPoolFee()),
                poolCalculationResult.getOptimalPoolCount(),
                poolCalculationResult.getInfluenceParam(),
                poolCalculationResult.getRelativeStakeOfPool(),
                poolCalculationResult.getRelativeStakeOfPoolOwner(),
                poolCalculationResult.getPoolPerformance()
        ).divide(new BigDecimal("0.8"), mathContext);
        totalRewardPotInAda = totalRewardPot.setScale(0, mathContext.getRoundingMode());

        System.out.println("Calculating total reward pot for epoch: " + epoch);
        System.out.println("The formula gives a total reward pot of: " +
                poolCalculationResult.getTotalRewardPot().setScale(0, mathContext.getRoundingMode()));
        System.out.println("But the reverse formula with the expected pool reward gives a total reward pot of: " +
                totalRewardPotInAda);
        System.out.println("The difference is: " +
                poolCalculationResult.getTotalRewardPot().setScale(0, mathContext.getRoundingMode()).subtract(totalRewardPotInAda));

        String poolId2 = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        poolCalculationResult = Test_calculatePoolReward(poolId2, epoch, true, totalRewardPotInAda);
        BigDecimal difference = poolCalculationResult.getActualPoolReward().setScale(0, mathContext.getRoundingMode())
                .subtract(poolCalculationResult.getExpectedPoolReward().setScale(0, mathContext.getRoundingMode()));

        System.out.println("Difference between expected pool reward and actual pool reward: " + difference.abs());
        assert(difference.abs().compareTo(new BigDecimal("25")) < 0);
    }
}
