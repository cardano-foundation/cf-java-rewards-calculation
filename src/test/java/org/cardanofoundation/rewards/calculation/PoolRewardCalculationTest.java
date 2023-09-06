package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.KoiosDataProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.epoch.model.EpochParams;
import rest.koios.client.backend.api.network.model.Totals;
import rest.koios.client.backend.api.pool.model.PoolHistory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
public class PoolRewardCalculationTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;

    void Test_calculatePoolReward(String poolId, int epoch) throws ApiException {
        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210
        PoolHistory poolHistoryCurrentEpoch = koiosDataProvider.getPoolHistory(poolId, epoch);
        BigDecimal poolStake = lovelaceToAda(new BigDecimal(poolHistoryCurrentEpoch.getActiveStake()));
        BigDecimal estimatedPoolReward = lovelaceToAda(new BigDecimal(poolHistoryCurrentEpoch.getDelegRewards()));
        BigDecimal poolFees = lovelaceToAda(new BigDecimal(poolHistoryCurrentEpoch.getPoolFees()));
        int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCnt();

        // Step 2: Get Epoch information of current epoch
        // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
        int totalBlocksInEpoch = koiosDataProvider.getTotalBlocksInEpoch(epoch);

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
                PoolRewardCalculation.calculateApparentPoolPerformance(poolStake, adaInCirculation,
                        blocksPoolHasMinted, totalBlocksInEpoch, decentralizationParam);

        // Step 7: Calculate total available reward for pools (total reward pot after treasury cut)
        BigDecimal totalRewardPot = lovelaceToAda(TreasuryCalculation.calculateTotalRewardPot(monetaryExpandRate, reserves, totalFeesInEpoch));
        BigDecimal stakePoolRewardsPot = totalRewardPot.multiply(new BigDecimal(1 - treasuryGrowRate));

        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"

        // Step 8: Get the latest pool update before this epoch and extract the pledge
        BigDecimal poolOwnerActiveStake = lovelaceToAda(new BigDecimal(
                koiosDataProvider.getLatestPoolUpdateBeforeOrInEpoch(poolId, epoch - 1).getPledge()));

        BigDecimal relativeStakeOfPoolOwner = poolOwnerActiveStake.divide(adaInCirculation, 20, RoundingMode.DOWN);
        BigDecimal relativePoolStake = poolStake.divide(adaInCirculation, 20, RoundingMode.DOWN);

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
        Assertions.assertEquals(estimatedPoolReward.setScale(0, RoundingMode.DOWN),
                poolReward.subtract(poolFees).setScale(0, RoundingMode.DOWN));
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

        //Test_calculatePoolReward(poolId1, epoch);
        Test_calculatePoolReward(poolId2, epoch);
        Test_calculatePoolReward(poolId3, epoch);
    }
}
