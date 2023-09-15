package org.cardanofoundation.rewards.calculation;

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
    public static double calculateApparentPoolPerformance(final double activePoolStake, final double totalActiveEpochStake, final int blocksMintedByPool, final int totalBlocksInEpoch, final double decentralizationParam) {
        if (decentralizationParam >= 0.8) {
            return 1.0;
        } else if (activePoolStake == 0.0 || totalActiveEpochStake == 0.0) {
            return 0.0;
        } else {
            final double blocksMintedInNonOBFTSlots = (double) totalBlocksInEpoch * (1.0 - decentralizationParam);
            final double relativeBlocksCreatedInEpoch = blocksMintedByPool / blocksMintedInNonOBFTSlots;
            final double relativeActiveStake = activePoolStake / totalActiveEpochStake;
            return relativeBlocksCreatedInEpoch / relativeActiveStake;
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
    public static double calculateOptimalPoolReward(double totalAvailableRewards, int optimalPoolCount, double influence, double relativeStakeOfPool, double relativeStakeOfPoolOwner) {

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
        double relativeStakeOfSaturatedPool = cappedRelativeStakeOfPoolOwner * ((sizeOfASaturatedPool - cappedRelativeStake) / sizeOfASaturatedPool);

        // o' - ((s' * (z0 - o')/ z0) / z0)
        double saturatedPoolWeight = cappedRelativeStake - (relativeStakeOfSaturatedPool / sizeOfASaturatedPool);

        // R/(1+a0) (s'a0(o' - (s'(z0 - o') / z0)) / z0)
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
    public static double calculateRewardPotByOptimalPoolReward(double poolReward, int optimalPoolCount, double influence, double relativeStakeOfPool, double relativeStakeOfPoolOwner, double poolPerformance) {

        double sizeOfASaturatedPool = 1.0 / optimalPoolCount;
        double cappedRelativeStake = Math.min(sizeOfASaturatedPool, relativeStakeOfPool);
        double cappedRelativeStakeOfPoolOwner = Math.min(sizeOfASaturatedPool, relativeStakeOfPoolOwner);

        // s' * a0
        double influenceOfOwner = cappedRelativeStakeOfPoolOwner * influence;

        // s'((z0 - o')/ z0)
        double relativeStakeOfSaturatedPool = cappedRelativeStakeOfPoolOwner * ((sizeOfASaturatedPool - cappedRelativeStake) / sizeOfASaturatedPool);

        // o' - (s' * (z0 - o')/ z0) / z0)
        double saturatedPoolWeight = (cappedRelativeStake - relativeStakeOfSaturatedPool) / sizeOfASaturatedPool;


        double poolWeightsWithPerformance = poolPerformance * (cappedRelativeStake + influenceOfOwner * saturatedPoolWeight);
        return ((1.0 + influence) * poolReward) / poolWeightsWithPerformance;
    }
}
