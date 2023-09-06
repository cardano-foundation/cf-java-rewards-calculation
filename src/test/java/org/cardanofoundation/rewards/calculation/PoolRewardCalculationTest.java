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

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_ADA;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ComponentScan
public class PoolRewardCalculationTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;


    @Test
    void Test_GetPoolPerformance() {
        // https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=220

        int blocksPoolHasMinted = 10;
        BigDecimal totalEpochStake = new BigDecimal("87498030468343");
        BigDecimal poolStake = new BigDecimal("27523186299296");

        double decentralizationParam = 0.64;
        int totalBlock = 21627;

        var apparentPoolPerformance =
                PoolRewardCalculation.calculateApparentPoolPerformance(poolStake, totalEpochStake,
                        blocksPoolHasMinted, totalBlock, decentralizationParam);
        assertEquals(new BigDecimal("0.296414596464228188354493042058"), apparentPoolPerformance);
    }

    @Test
    void Test_GetPoolReward() throws ApiException {
        // Pool information of pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt in epoch 211
        // Source: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=211
        int blocksPoolHasMinted = 4;
        BigDecimal poolStake = lovelaceToAda(new BigDecimal("27092487965405"));
        BigDecimal relativePoolStake = new BigDecimal("0.002661916729491990325");
        BigDecimal estimatedPoolReward = lovelaceToAda(new BigDecimal("20268536110")); // as deleg_rewards

        // Epoch information of epoch 211
        // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
        //BigDecimal totalEpochStake = lovelaceToAda(new BigDecimal("10177811974822904"));
        int totalBlocksInEpoch = 21315;

        // Fee and reserves in epoch 211 (koios seems to have an offset of 2 epochs here)
        BigDecimal totalFeesInEpoch = new BigDecimal(koiosDataProvider.getTotalFeesForEpoch(209));

        Totals adaPotsForPreviousEpoch = koiosDataProvider.getAdaPotsForEpoch(210);
        Totals adaPotsForCurrentEpoch = koiosDataProvider.getAdaPotsForEpoch(211);
        BigDecimal reserves = new BigDecimal(adaPotsForPreviousEpoch.getReserves());

        //assertEquals(relativePoolStake, poolStake.divide(totalEpochStake, 21, RoundingMode.DOWN));
        BigDecimal adaInCirculation = lovelaceToAda(new BigDecimal(adaPotsForPreviousEpoch.getCirculation()));

        EpochParams epochParams = koiosDataProvider.getProtocolParametersForEpoch(211);
        double decentralizationParam = epochParams.getDecentralisation().doubleValue();
        int optimalPoolCount = epochParams.getOptimalPoolCount();
        double influenceParam = epochParams.getInfluence().doubleValue();
        double monetaryExpandRate = epochParams.getMonetaryExpandRate().doubleValue();
        double treasuryGrowRate = epochParams.getTreasuryGrowthRate().doubleValue();

        var apparentPoolPerformance =
                PoolRewardCalculation.calculateApparentPoolPerformance(poolStake, adaInCirculation,
                        blocksPoolHasMinted, totalBlocksInEpoch, decentralizationParam);

        // BigDecimal totalRewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
        //        monetaryExpandRate, totalBlocksInEpoch, reserves, totalFeesInEpoch);
        BigDecimal totalRewardPot = lovelaceToAda(TreasuryCalculation.calculateTotalRewardPot(monetaryExpandRate, reserves, totalFeesInEpoch));
        Assertions.assertEquals(new BigDecimal("39842259.004735179"), totalRewardPot.setScale(9, RoundingMode.DOWN));

        BigDecimal stakePoolRewardsPot = totalRewardPot.multiply(new BigDecimal(1 - treasuryGrowRate));

        // shelley-delegation.pdf 5.5.3
        //      the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration
        //
        // https://api.koios.rest/api/v0/pool_updates?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&active_epoch_no=lte.211
        BigDecimal poolOwnerActiveStake = lovelaceToAda(new BigDecimal("450000000000"));
        BigDecimal relativeStakeOfPoolOwner = poolOwnerActiveStake.divide(adaInCirculation, 20, RoundingMode.DOWN);
        relativePoolStake = (poolOwnerActiveStake.add(poolStake)).divide(adaInCirculation, 20, RoundingMode.DOWN);

        BigDecimal optimalPoolReward =
                PoolRewardCalculation.calculateOptimalPoolReward(
                        stakePoolRewardsPot,
                        optimalPoolCount,
                        influenceParam,
                        relativePoolStake,
                        relativeStakeOfPoolOwner);

        System.out.println("Estimated pool reward: " + estimatedPoolReward);

        BigDecimal poolReward = PoolRewardCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);
        System.out.println("Calculated pool reward: " + poolReward);
        System.out.println("Difference: " + estimatedPoolReward.subtract(poolReward));
        Assertions.assertEquals(estimatedPoolReward, poolReward);
    }
}
