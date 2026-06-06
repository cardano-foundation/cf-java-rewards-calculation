package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.util.Ratio;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreasuryCalculationTest {

    @Test
    void calculateTotalRewardPotWithEtaUsesExactRationalFloor() {
        BigInteger rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                new BigDecimal("0.003"),
                59,
                BigDecimal.ZERO,
                new BigInteger("35989500000000000"),
                BigInteger.ZERO,
                shortEpochConfig());

        assertEquals(new BigInteger("106169025000000"), rewardPot);
    }

    @Test
    void calculateTotalRewardPotWithEtaFloorsFractionalResult() {
        BigInteger rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                new BigDecimal("0.003"),
                59,
                BigDecimal.ZERO,
                BigInteger.valueOf(1000),
                BigInteger.valueOf(7),
                shortEpochConfig());

        assertEquals(BigInteger.valueOf(9), rewardPot);
    }

    @Test
    void calculateTotalRewardPotWithEtaUsesEtaOneWhenDecentralizationAtThreshold() {
        BigInteger rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                new BigDecimal("0.003"),
                0,
                new BigDecimal("0.8"),
                BigInteger.valueOf(1000),
                BigInteger.ZERO,
                shortEpochConfig());

        assertEquals(BigInteger.valueOf(3), rewardPot);
    }

    @Test
    void calculateTotalRewardPotWithEtaCapsEtaAtOneWhenBlocksMeetExpectation() {
        BigInteger rewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                new BigDecimal("0.003"),
                60,
                BigDecimal.ZERO,
                BigInteger.valueOf(1000),
                BigInteger.ZERO,
                shortEpochConfig());

        assertEquals(BigInteger.valueOf(3), rewardPot);
    }

    @Test
    void calculateTotalRewardPotWithEtaReportsExpectedBlocksConfiguration() {
        NetworkConfig networkConfig = NetworkConfig.builder()
                .expectedSlotsPerEpoch(0)
                .activeSlotCoefficient(1.0)
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                TreasuryCalculation.calculateTotalRewardPotWithEta(
                        new BigDecimal("0.003"),
                        1,
                        new BigDecimal("0.79"),
                        BigInteger.valueOf(1000),
                        BigInteger.ZERO,
                        networkConfig));

        assertTrue(exception.getMessage().contains("slotsPerEpoch=0"));
        assertTrue(exception.getMessage().contains("decentralization=0.79"));
    }

    @Test
    void ratioMultiplyAndFloorUsesExactDecimalValue() {
        BigInteger treasuryCut = Ratio.from(new BigDecimal("0.2"))
                .multiplyAndFloor(new BigInteger("106169025000000"));

        assertEquals(new BigInteger("21233805000000"), treasuryCut);
    }

    @Test
    void ratioFloorHandlesNegativeRemainders() {
        assertEquals(BigInteger.valueOf(-3), Ratio.of(BigInteger.valueOf(-7), BigInteger.valueOf(3)).floor());
        assertEquals(BigInteger.valueOf(-3), Ratio.from(new BigDecimal("-0.2")).multiplyAndFloor(BigInteger.valueOf(11)));
    }

    private NetworkConfig shortEpochConfig() {
        return NetworkConfig.builder()
                .expectedSlotsPerEpoch(60)
                .activeSlotCoefficient(1.0)
                .build();
    }
}
