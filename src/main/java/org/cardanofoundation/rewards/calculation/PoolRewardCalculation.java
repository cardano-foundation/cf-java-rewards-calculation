package org.cardanofoundation.rewards.calculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.cardanofoundation.rewards.constants.RewardConstants.EXPECTED_BLOCKS_PER_EPOCH;

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
    public static double calculateApparentPoolPerformance(double poolStake, double totalEpochStake,
                                                          int blocksMintedByPool, int totalBlocksInEpoch,
                                                          double decentralizationParam) {
        if (decentralizationParam >= 0.8) {
            return 1.0;
        }

        double blocksMintedInNonOBFTSlots = totalBlocksInEpoch * (1 - decentralizationParam);

        double relativeBlocksCreatedInEpoch = blocksMintedByPool / blocksMintedInNonOBFTSlots;
        double relativeActiveStake = poolStake / totalEpochStake;
        return relativeBlocksCreatedInEpoch / relativeActiveStake;
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
    public static double calculateOptimalPoolReward(
            double totalAvailableRewards,
            int optimalPoolCount,
            double influence,
            double relativeStakeOfPool,
            double relativeStakeOfPoolOwner) {

        double sizeOfASaturatedPool = 1.0 / optimalPoolCount;
        double cappedRelativeStake = Math.min(relativeStakeOfPool, sizeOfASaturatedPool);
        double cappedRelativeStakeOfPoolOwner = Math.min(relativeStakeOfPoolOwner, sizeOfASaturatedPool);

        // R / (1 + a0)
        // "R are the total available rewards for the epoch (in ada)." (shelley-delegation.pdf 5.5.3)
        double totalAvailableRewardsInAda = Math.round(totalAvailableRewards);
        double rewardsDividedByOnePlusInfluence = totalAvailableRewardsInAda / (1 + influence);

        // s' * a0
        double influenceOfOwner = cappedRelativeStakeOfPoolOwner * influence;

        // s'((z0 - o')/ z0)
        double relativeStakeOfSaturatedPool = cappedRelativeStakeOfPoolOwner * (
                (sizeOfASaturatedPool - cappedRelativeStake) / sizeOfASaturatedPool);

        // o' - ((s' * (z0 - o')/ z0) / z0)
        double saturatedPoolWeight = cappedRelativeStake - (relativeStakeOfSaturatedPool / sizeOfASaturatedPool);

        return rewardsDividedByOnePlusInfluence * (cappedRelativeStake + influenceOfOwner * saturatedPoolWeight);
    }

    /*
     *  Calculate the pool reward with the formula (shelley-delegation.pdf 5.5.3 page 37 below):
     *  actualRewards = poolPerformance * optimalPoolReward
     */
    public static double calculatePoolReward(double optimalPoolReward, double poolPerformance) {
        return optimalPoolReward * poolPerformance;
    }

    /*
     *  Calculate the total reward pot (R) with transposing the equation from above:
     */
    public static double calculateRewardPotByOptimalPoolReward(
            double poolReward,
            int optimalPoolCount,
            double influence,
            double relativeStakeOfPool,
            double relativeStakeOfPoolOwner,
            double poolPerformance) {

        double sizeOfASaturatedPool = 1.0 / optimalPoolCount;
        double cappedRelativeStake = Math.min(sizeOfASaturatedPool, relativeStakeOfPool);
        double cappedRelativeStakeOfPoolOwner = Math.min(sizeOfASaturatedPool, relativeStakeOfPoolOwner);
        // s' * a0
        double influenceOfOwner = cappedRelativeStakeOfPoolOwner * influence;

        // s'((z0 - o')/ z0)
        double relativeStakeOfSaturatedPool = cappedRelativeStakeOfPoolOwner * (
                (sizeOfASaturatedPool - cappedRelativeStake) / sizeOfASaturatedPool);

        // o' - (s' * (z0 - o')/ z0) / z0)
        double saturatedPoolWeight = (cappedRelativeStake - relativeStakeOfSaturatedPool) / sizeOfASaturatedPool;


        double poolWeightsWithPerformance = poolPerformance * (cappedRelativeStake + influenceOfOwner * saturatedPoolWeight);
        return ((1.0 + influence) * poolReward) / poolWeightsWithPerformance;
    }
}
