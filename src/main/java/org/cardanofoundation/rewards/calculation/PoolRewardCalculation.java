package org.cardanofoundation.rewards.calculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

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
    public static BigDecimal calculateApparentPoolPerformance(BigDecimal poolStake, BigDecimal totalEpochStake,
                                                              int blocksMintedByPool, int totalBlocksInEpoch,
                                                              double decentralizationParam) {
        if (decentralizationParam >= 0.8) {
            return BigDecimal.ONE;
        }

        BigDecimal relativeBlocksCreatedInEpoch = new BigDecimal(blocksMintedByPool).divide(new BigDecimal(totalBlocksInEpoch), 30, RoundingMode.DOWN);
        BigDecimal relativeActiveStake = poolStake.divide(totalEpochStake, 30, RoundingMode.DOWN);
        return relativeBlocksCreatedInEpoch.divide(relativeActiveStake, 30, RoundingMode.DOWN);
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
        var influence = new BigDecimal(influenceParam);
        var sizeOfASaturatedPool =
                BigDecimal.ONE.divide(
                        new BigDecimal(optimalPoolCount), 30, RoundingMode.DOWN);

        var cappedRelativeStake = sizeOfASaturatedPool.min(relativeStakeOfPool);
        var cappedRelativeStakeOfPoolOwner = sizeOfASaturatedPool.min(relativeStakeOfPoolOwner);
        MathContext mathContext = new MathContext(30, RoundingMode.DOWN);

        // R / (1 + a0)
        var rewardsDividedByOnePlusInfluence = totalAvailableRewards.divide(BigDecimal.ONE.add(influence), 30, RoundingMode.DOWN);

        // s' * a0
        var influenceOfOwner = cappedRelativeStakeOfPoolOwner.multiply(influence, mathContext);

        // s'((z0 - o')/ z0)
        var relativeStakeOfSaturatedPool = cappedRelativeStakeOfPoolOwner.multiply(
                sizeOfASaturatedPool.subtract(cappedRelativeStake, mathContext).divide(sizeOfASaturatedPool, 30, RoundingMode.DOWN));

        // o' - (s' * (z0 - o')/ z0) / z0)
        var saturatedPoolWeight = cappedRelativeStake.subtract(relativeStakeOfSaturatedPool, mathContext)
                .divide(sizeOfASaturatedPool, 30, RoundingMode.DOWN);

        return rewardsDividedByOnePlusInfluence.multiply(cappedRelativeStake.add(influenceOfOwner.multiply(saturatedPoolWeight)));
    }

    /*
     *  Calculate the pool reward with the formula (shelley-delegation.pdf 5.5.3 page 37 below):
     *  actualRewards = poolPerformance * optimalPoolReward
     */
    public static BigDecimal calculatePoolReward(BigDecimal optimalPoolReward, BigDecimal poolPerformance) {
        return optimalPoolReward.multiply(poolPerformance);
    }
}
