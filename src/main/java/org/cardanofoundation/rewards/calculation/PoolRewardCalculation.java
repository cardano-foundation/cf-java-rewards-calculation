package org.cardanofoundation.rewards.calculation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.cardanofoundation.rewards.constants.RewardConstants.EXPECTED_BLOCKS_PER_EPOCH;
import static org.cardanofoundation.rewards.util.CurrencyConverter.FLOOR_MATH_CONTEXT;
import static org.cardanofoundation.rewards.util.CurrencyConverter.HALF_UP_MATH_CONTEXT;

public class PoolRewardCalculation {

    /*
    * https://github.com/input-output-hk/cardano-ledger/releases/latest/download/shelley-delegation.pdf
    *
    * Calculate the apparent pool performance with the formula (shelley-delegation.pdf page 36):
    *
    * performance = relativeBlocksCreatedInEpoch / relativeActiveStake
    *
    * hint: shelley-delegation.pdf 3.8.3
    *       As long as we have d >= 0.8, we set the apparent performance of any pool to 1
    */
    public static BigDecimal calculateApparentPoolPerformance(final BigDecimal activePoolStake, final BigDecimal totalActiveEpochStake,
                                                              final int blocksMintedByPool, final int totalBlocksInEpoch,
                                                              final double decentralizationParam) {
        if (decentralizationParam >= 0.8) {
            return BigDecimal.ONE;
        } else if (activePoolStake.equals(BigDecimal.ZERO) || totalActiveEpochStake.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        } else {
            // expected apparent performance 213 nordic = 1.21459564615226245304531128140, blocks calculated=4955 (4625 real non OBFT blocks) nonOBFT blocks OR relativeActiveStake = 0.000693029688042632188034502108054 (4% difference)
            // expected apparent performance 214 nordic = 0.560953633013609757215468303800, blocks calculated=5669 (5120 real non OBFT blocks) nonOBFT blocks OR  relativeActiveStake = 0.000687761779994465547676440116621 (8% difference)
            // expected apparent performance 215 nordic = 0.844558550914722447307672757588, 6176 nonOBFT blocks OR  relativeActiveStake = 0.000632505625109670755947435186226 (10% difference)
            final BigDecimal expectedBlocks = BigDecimal.valueOf(EXPECTED_BLOCKS_PER_EPOCH).multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(decentralizationParam)));
            final BigDecimal relativeBlocksCreatedInEpoch = BigDecimal.valueOf(blocksMintedByPool).divide(expectedBlocks.max(BigDecimal.ONE), HALF_UP_MATH_CONTEXT);
            final BigDecimal relativeActiveStake = totalActiveEpochStake.divide(totalActiveEpochStake, HALF_UP_MATH_CONTEXT);
            return relativeBlocksCreatedInEpoch.divide(relativeActiveStake, RoundingMode.HALF_UP);
        }
    }

    /*
     * https://github.com/input-output-hk/cardano-ledger/releases/latest/download/shelley-delegation.pdf
     *
     * Calculate the pool reward with the formula (shelley-delegation.pdf 5.5.3):
     *
     * optimalPoolCount (nOpt "k") and influence (a0) are
     * protocol parameters: https://cips.cardano.org/cips/cip9/
     *
     * sizeOfASaturatedPool = 1 / optimalPoolCount
     * cappedRelativeStake = min(relativeStakeOfPool, sizeOfASaturatedPool)
     * cappedRelativeStakeOfPoolOwner = min(relativeStakeOfPoolOwner, sizeOfASaturatedPool)
     *
     * rewards = (totalAvailableRewards / (1 + influence)) * (
     *      cappedRelativeStake +
     *      cappedRelativeStakeOfPoolOwner * influence * ((
     *          cappedRelativeStake - cappedRelativeStakeOfPoolOwner *
     *          (( sizeOfASaturatedPool - cappedRelativeStake ) / sizeOfASaturatedPool))
     *          / sizeOfASaturatedPool)
     *      )
     */
    public static BigDecimal calculateOptimalPoolReward(
            BigDecimal totalAvailableRewards,
            int optimalPoolCount,
            double influenceParam,
            BigDecimal relativeStakeOfPool,
            BigDecimal relativeStakeOfPoolOwner) {
        final BigDecimal influence = new BigDecimal(String.valueOf(influenceParam), HALF_UP_MATH_CONTEXT);
        final BigDecimal sizeOfASaturatedPool = BigDecimal.ONE.divide(BigDecimal.valueOf(optimalPoolCount), HALF_UP_MATH_CONTEXT);
        final BigDecimal cappedRelativeStake = sizeOfASaturatedPool.min(relativeStakeOfPool);
        final BigDecimal cappedRelativeStakeOfPoolOwner = sizeOfASaturatedPool.min(relativeStakeOfPoolOwner);

        // R / (1 + a0)
        // "R are the total available rewards for the epoch (in ada)." (shelley-delegation.pdf 5.5.3)
        final BigDecimal totalAvailableRewardsInAda = totalAvailableRewards.round(FLOOR_MATH_CONTEXT);
        final BigDecimal rewardsDividedByOnePlusInfluence = totalAvailableRewardsInAda.divide(BigDecimal.ONE.add(influence), HALF_UP_MATH_CONTEXT);

        // s' * a0
        final BigDecimal influenceOfOwner = cappedRelativeStakeOfPoolOwner.multiply(influence, HALF_UP_MATH_CONTEXT);

        // s'((z0 - o')/ z0)
        final BigDecimal relativeStakeOfSaturatedPool = cappedRelativeStakeOfPoolOwner.multiply(sizeOfASaturatedPool.subtract(cappedRelativeStake).divide(sizeOfASaturatedPool, HALF_UP_MATH_CONTEXT), HALF_UP_MATH_CONTEXT);

        // (o' - (s'(z0 - o') / z0)) / z0
        final BigDecimal saturatedPoolWeight = cappedRelativeStake
                .subtract(relativeStakeOfSaturatedPool, HALF_UP_MATH_CONTEXT)
                .divide(sizeOfASaturatedPool, RoundingMode.HALF_UP);

        // R/(1+a0) (s'a0(o' - (s'(z0 - o') / z0)) / z0)
        return rewardsDividedByOnePlusInfluence.multiply(cappedRelativeStake.add(influenceOfOwner.multiply(saturatedPoolWeight)));
    }

    /*
     *  Calculate the pool reward with the formula (shelley-delegation.pdf 5.5.3 page 37 below):
     *  actualRewards = poolPerformance * optimalPoolReward
     */
    public static BigDecimal calculatePoolReward(BigDecimal optimalPoolReward, BigDecimal poolPerformance) {
        return optimalPoolReward.multiply(poolPerformance);
    }

    /*
     *  Calculate the total reward pot (R) with transposing the equation from above:
     */
    public static BigDecimal calculateRewardPotByOptimalPoolReward(
            BigDecimal poolReward,
            int optimalPoolCount,
            double influenceParam,
            BigDecimal relativeStakeOfPool,
            BigDecimal relativeStakeOfPoolOwner,
            BigDecimal poolPerformance) {
        MathContext mathContext = new MathContext(30, RoundingMode.FLOOR);
        var influence = new BigDecimal(influenceParam);
        var sizeOfASaturatedPool =
                BigDecimal.ONE.divide(
                        new BigDecimal(optimalPoolCount), mathContext);
        var cappedRelativeStake = sizeOfASaturatedPool.min(relativeStakeOfPool);
        var cappedRelativeStakeOfPoolOwner = sizeOfASaturatedPool.min(relativeStakeOfPoolOwner);
        // s' * a0
        var influenceOfOwner = cappedRelativeStakeOfPoolOwner.multiply(influence, mathContext);

        // s'((z0 - o')/ z0)
        var relativeStakeOfSaturatedPool = cappedRelativeStakeOfPoolOwner.multiply(
                sizeOfASaturatedPool.subtract(cappedRelativeStake, mathContext).divide(sizeOfASaturatedPool, mathContext));

        // o' - (s' * (z0 - o')/ z0) / z0)
        var saturatedPoolWeight = cappedRelativeStake.subtract(relativeStakeOfSaturatedPool, mathContext)
                .divide(sizeOfASaturatedPool, mathContext);

        var poolWeightsWithPerformance = poolPerformance.multiply(cappedRelativeStake.add(influenceOfOwner.multiply(saturatedPoolWeight)));
        return ((BigDecimal.ONE.add(influence)).multiply(poolReward)).divide(poolWeightsWithPerformance, mathContext);
    }
}
