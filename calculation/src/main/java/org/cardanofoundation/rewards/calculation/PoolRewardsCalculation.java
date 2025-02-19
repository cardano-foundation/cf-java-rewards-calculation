package org.cardanofoundation.rewards.calculation;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.*;

@Slf4j
public class PoolRewardsCalculation {

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
    public static BigDecimal calculateApparentPoolPerformance(final BigInteger activePoolStake, final BigInteger totalActiveEpochStake, final int blocksMintedByPool, final int blocksMintedByStakePools, final BigDecimal decentralizationParam) {
        BigDecimal poolStake = new BigDecimal(activePoolStake);
        BigDecimal totalEpochStake = new BigDecimal(totalActiveEpochStake);

        if (decentralizationParam.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
            return BigDecimal.ONE;
        } else if (isZero(poolStake) || isZero(totalEpochStake)) {
            return BigDecimal.ZERO;
        } else {
            final BigDecimal relativeBlocksCreatedInEpoch = divide(blocksMintedByPool, blocksMintedByStakePools);
            final BigDecimal relativeActiveStake = divide(poolStake, totalEpochStake);
            return divide(relativeBlocksCreatedInEpoch, relativeActiveStake);
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
    public static BigInteger calculateOptimalPoolReward(BigInteger totalAvailableRewards, int optimalPoolCount, BigDecimal influence, BigDecimal relativeStakeOfPool, BigDecimal relativeStakeOfPoolOwner) {

        BigDecimal sizeOfASaturatedPool = divide(BigDecimal.ONE, optimalPoolCount);
        BigDecimal cappedRelativeStake = min(relativeStakeOfPool, sizeOfASaturatedPool);
        BigDecimal cappedRelativeStakeOfPoolOwner = min(relativeStakeOfPoolOwner, sizeOfASaturatedPool);

        // R / (1 + a0)
        // "R are the total available rewards for the epoch (in ada)." (shelley-delegation.pdf 5.5.3)
        BigDecimal rewardsDividedByOnePlusInfluence = divide(totalAvailableRewards, add(BigDecimal.ONE, influence));

        // (z0 - sigma') / z0
        BigDecimal relativeStakeOfSaturatedPool = divide(subtract(sizeOfASaturatedPool, cappedRelativeStake), sizeOfASaturatedPool);

        // (sigma' - s' * relativeStakeOfSaturatedPool) / z0
        BigDecimal saturatedPoolWeight = divide(subtract(cappedRelativeStake, multiply(cappedRelativeStakeOfPoolOwner, relativeStakeOfSaturatedPool)), sizeOfASaturatedPool);

        // R / (1+a0) * (sigma' + s' * a0 * saturatedPoolWeight)
        return floor(multiply(rewardsDividedByOnePlusInfluence,
                add(cappedRelativeStake, multiply(cappedRelativeStakeOfPoolOwner, influence, saturatedPoolWeight))));
    }

    /*
     *  Calculate the pool reward with the formula (shelley-delegation.pdf 5.5.3 page 37 below):
     *  actualRewards = poolPerformance * optimalPoolReward
     */
    public static BigInteger calculatePoolReward(BigInteger optimalPoolReward, BigDecimal poolPerformance) {
        return floor(multiply(optimalPoolReward, poolPerformance));
    }

    /*
     * This method calculates the pool operator reward regarding the formula described
     * in the shelly-ledger.pdf p. 61, figure 47
     */
    public static BigInteger calculateLeaderReward(BigInteger poolReward, BigDecimal margin, BigInteger poolCost,
                                                   BigDecimal relativeOwnerStake, BigDecimal relativeStakeOfPool) {
        if (isLowerOrEquals(poolReward, poolCost)) {
            return poolReward;
        }

        return add(poolCost, floor(multiply(subtract(poolReward, poolCost),
                add(margin, multiply(BigDecimal.ONE.subtract(margin), divide(relativeOwnerStake, relativeStakeOfPool))))));
    }

    /*
     * This method calculates the pool member reward regarding the formula described
     * in the shelly-ledger.pdf p. 61, figure 47
     *
     * See Haskell implementation: https://github.com/input-output-hk/cardano-ledger/blob/aed5dde9cd1096cfc2e255879cd617c0d64f8d9d/eras/shelley/impl/src/Cardano/Ledger/Shelley/Rewards.hs#L117
     */
    public static BigInteger calculateMemberReward(BigInteger poolReward, BigDecimal margin, BigInteger poolCost,
                                                   BigDecimal relativeMemberStake, BigDecimal relativeStakeOfPool) {
        if (isLowerOrEquals(poolReward, poolCost)) {
            return BigInteger.ZERO;
        }

        return floor(divide(multiply(
                poolReward.subtract(poolCost),
                subtract(BigDecimal.ONE, margin),
                relativeMemberStake), relativeStakeOfPool));
    }

    public static PoolRewardCalculationResult calculatePoolRewardInEpoch(final String poolId, final PoolState poolStateCurrentEpoch,
                                                                         final int totalBlocksInEpoch, final ProtocolParameters protocolParameters,
                                                                         final BigInteger adaInCirculation, final BigInteger activeStakeInEpoch, BigInteger stakePoolRewardsPot,
                                                                         final BigInteger totalActiveStakeOfOwners, final Set<String> poolOwnerStakeAddresses,
                                                                         final Set<String> deregisteredAccounts, final boolean ignoreLeaderReward,
                                                                         final Set<String> lateDeregisteredAccounts,
                                                                         final Set<String> accountsRegisteredInThePast,
                                                                         final NetworkConfig networkConfig) {
        final int earnedEpoch = poolStateCurrentEpoch.getEpoch();
        final PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculationResult.builder()
                .epoch(earnedEpoch)
                .poolId(poolId)
                .poolReward(BigInteger.ZERO)
                .distributedPoolReward(BigInteger.ZERO)
                .unspendableEarnedRewards(BigInteger.ZERO)
                .build();

        /*
            babbage-ledger.pdf | 6 Forgo Reward Calculation Prefilter | p. 14

            The reward calculation no longer filters out the unregistered stake credentials when creating
            a reward update. As in the Shelley era, though, they are still filtered on the epoch boundary
            when the reward update is applied
        */
        if (earnedEpoch >= networkConfig.getVasilHardforkEpoch()) {
            lateDeregisteredAccounts.addAll(deregisteredAccounts);
            deregisteredAccounts.clear();
        }

        final BigInteger poolStake = poolStateCurrentEpoch.getActiveStake();
        final BigInteger poolPledge = poolStateCurrentEpoch.getPledge();
        final BigDecimal poolMargin = BigDecimal.valueOf(poolStateCurrentEpoch.getMargin());
        final BigInteger poolFixedCost = poolStateCurrentEpoch.getFixedCost();
        final int blocksPoolHasMinted = poolStateCurrentEpoch.getBlockCount();

        poolRewardCalculationResult.setPoolMargin(poolStateCurrentEpoch.getMargin());
        poolRewardCalculationResult.setPoolCost(poolFixedCost);
        poolRewardCalculationResult.setRewardAddress(poolStateCurrentEpoch.getRewardAddress());

        if (blocksPoolHasMinted == 0) {
            return poolRewardCalculationResult;
        }

        BigDecimal decentralizationParameter = protocolParameters.getDecentralisation();
        int optimalPoolCount = protocolParameters.getOptimalPoolCount();
        BigDecimal influenceParam = protocolParameters.getPoolOwnerInfluence();

        // Calculate apparent pool performance
        final BigDecimal apparentPoolPerformance =
                PoolRewardsCalculation.calculateApparentPoolPerformance(poolStake, activeStakeInEpoch,
                        blocksPoolHasMinted, totalBlocksInEpoch, decentralizationParameter);
        poolRewardCalculationResult.setApparentPoolPerformance(apparentPoolPerformance);
        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"
        poolRewardCalculationResult.setPoolOwnerStakeAddresses(poolOwnerStakeAddresses);

        if (isLower(totalActiveStakeOfOwners, poolPledge)) {
            return poolRewardCalculationResult;
        }

        final BigDecimal relativeStakeOfPoolOwner = divide(poolPledge, adaInCirculation);
        final BigDecimal relativePoolStake = divide(poolStake, adaInCirculation);

        // Step 8: Calculate optimal pool reward
        final BigInteger optimalPoolReward =
                PoolRewardsCalculation.calculateOptimalPoolReward(
                        stakePoolRewardsPot,
                        optimalPoolCount,
                        influenceParam,
                        relativePoolStake,
                        relativeStakeOfPoolOwner);
        poolRewardCalculationResult.setOptimalPoolReward(optimalPoolReward);

        // Step 9: Calculate pool reward as optimal pool reward * apparent pool performance
        final BigInteger poolReward = PoolRewardsCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);
        poolRewardCalculationResult.setPoolReward(poolReward);

        // Step 10: Calculate pool operator reward
        BigInteger poolOperatorReward = PoolRewardsCalculation.calculateLeaderReward(poolReward, poolMargin, poolFixedCost,
                divide(totalActiveStakeOfOwners, adaInCirculation), relativePoolStake);

        BigInteger unspendableEarnedRewards = BigInteger.ZERO;
        String rewardAddress = poolRewardCalculationResult.getRewardAddress();

        if (!accountsRegisteredInThePast.contains(rewardAddress)) {
            log.info(poolRewardCalculationResult.getRewardAddress() + " has never been registered. Operator would have received " + poolOperatorReward + " but will not receive any rewards.");
            if (earnedEpoch >= networkConfig.getVasilHardforkEpoch()) {
                unspendableEarnedRewards = poolOperatorReward;
            }
            poolOperatorReward = BigInteger.ZERO;
        } else if (deregisteredAccounts.contains(rewardAddress)) {
            log.info(poolRewardCalculationResult.getRewardAddress() + " has been deregistered. Operator would have received " + poolOperatorReward + " but will not receive any rewards.");
            poolOperatorReward = BigInteger.ZERO;
        } else if (lateDeregisteredAccounts.contains(rewardAddress)) {
            log.info("[unregRU]: " + poolRewardCalculationResult.getRewardAddress() + " has been deregistered lately. Operator would have received " + poolOperatorReward + " but will not receive any rewards.");
            unspendableEarnedRewards = poolOperatorReward;
            poolOperatorReward = BigInteger.ZERO;
        }

        if (ignoreLeaderReward) {
            poolOperatorReward = BigInteger.ZERO;
            log.debug("[reward address of multiple pools] Pool " + poolId + " has been ignored. Operator would have received " + poolOperatorReward + " but will not receive any rewards.");
        }

        // Step 11: Calculate pool member reward
        BigInteger poolMemberRewards = BigInteger.ZERO;
        final HashSet<Reward> memberRewards = new HashSet<>();
        for (Delegator delegator : poolStateCurrentEpoch.getDelegators()) {
            final String stakeAddress = delegator.getStakeAddress();

            /*
                "[...] the value of rewards in the reward function should be computed using an aggregating
                union so that leader rewards from multiple sources are aggregated.
                This was corrected at the Allegra hard fork"

                shelley-ledger.pdf | 17.4 Reward aggregation | p. 114
             */
            if (stakeAddress.equals(poolStateCurrentEpoch.getRewardAddress())
                    && earnedEpoch < networkConfig.getAllegraHardforkEpoch()) {
                continue;
            }

            if (poolOwnerStakeAddresses.contains(stakeAddress)) {
                continue;
            }

            BigInteger memberReward = PoolRewardsCalculation.calculateMemberReward(poolReward, poolMargin,
                    poolFixedCost, divide(delegator.getActiveStake(), adaInCirculation), relativePoolStake);

            if (deregisteredAccounts.contains(stakeAddress)) {
                log.debug("Delegator " + stakeAddress + " has been deregistered. Delegator would have received " + memberReward + " but will not receive any rewards.");
                memberReward = BigInteger.ZERO;
            } else if (lateDeregisteredAccounts.contains(stakeAddress)) {
                log.debug("[unregRU]: " + stakeAddress + " has been deregistered lately. Delegator would have received " + memberReward + " but will not receive any rewards.");
                unspendableEarnedRewards = unspendableEarnedRewards.add(memberReward);
                memberReward = BigInteger.ZERO;
            }

            memberRewards.add(Reward.builder()
                    .amount(memberReward)
                    .stakeAddress(stakeAddress)
                    .build());

            poolMemberRewards = poolMemberRewards.add(memberReward);
        }
        poolRewardCalculationResult.setDistributedPoolReward(poolOperatorReward.add(poolMemberRewards));
        poolRewardCalculationResult.setOperatorReward(poolOperatorReward);
        poolRewardCalculationResult.setMemberRewards(memberRewards);
        poolRewardCalculationResult.setUnspendableEarnedRewards(unspendableEarnedRewards);
        return poolRewardCalculationResult;
    }
}
