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
     *
     * See Haskell implementation: https://github.com/input-output-hk/cardano-ledger/blob/64459cc87094331c79d11880e0a4c81b9a721ab0/eras/shelley/impl/src/Cardano/Ledger/Shelley/Rewards.hs#L87C32-L87C44
     */
    public static double calculateApparentPoolPerformance(final double activePoolStake, final double totalActiveEpochStake, final int blocksMintedByPool, final int blocksMintedByStakePools, final double decentralizationParam) {
        if (decentralizationParam >= 0.8) {
            return 1.0;
        } else if (activePoolStake == 0.0 || totalActiveEpochStake == 0.0) {
            return 0.0;
        } else {

            final double relativeBlocksCreatedInEpoch = (double) blocksMintedByPool / (double) blocksMintedByStakePools;
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
     *
     * See the Haskell implementation: https://github.com/input-output-hk/cardano-ledger/blob/e722881568155fc39550a8dfabda3efeb263a1e5/shelley/chain-and-ledger/executable-spec/src/Shelley/Spec/Ledger/EpochBoundary.hs#L111
     */
    public static double calculateOptimalPoolReward(double totalAvailableRewards, int optimalPoolCount, double influence, double relativeStakeOfPool, double relativeStakeOfPoolOwner) {

        double sizeOfASaturatedPool = 1.0 / optimalPoolCount;
        double cappedRelativeStake = Math.min(relativeStakeOfPool, sizeOfASaturatedPool);
        double cappedRelativeStakeOfPoolOwner = Math.min(relativeStakeOfPoolOwner, sizeOfASaturatedPool);

        // R / (1 + a0)
        // "R are the total available rewards for the epoch (in ada)." (shelley-delegation.pdf 5.5.3)
        double rewardsDividedByOnePlusInfluence = totalAvailableRewards / (1 + influence);

        // (z0 - sigma') / z0
        double relativeStakeOfSaturatedPool = (sizeOfASaturatedPool - cappedRelativeStake) / sizeOfASaturatedPool;

        // (sigma' - s' * relativeStakeOfSaturatedPool) / z0
        double saturatedPoolWeight = (cappedRelativeStake - cappedRelativeStakeOfPoolOwner * relativeStakeOfSaturatedPool) / sizeOfASaturatedPool;

        // R / (1+a0) * (sigma' + s' * a0 * saturatedPoolWeight)
        return Math.floor(rewardsDividedByOnePlusInfluence *
                (cappedRelativeStake + cappedRelativeStakeOfPoolOwner * influence * saturatedPoolWeight));
    }

    /*
     *  Calculate the pool reward with the formula (shelley-delegation.pdf 5.5.3 page 37 below):
     *  actualRewards = poolPerformance * optimalPoolReward
     */
    public static double calculatePoolReward(double optimalPoolReward, double poolPerformance) {
        return optimalPoolReward * poolPerformance;
    }
}
