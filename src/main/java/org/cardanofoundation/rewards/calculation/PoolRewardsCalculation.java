package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.entity.jpa.projection.LatestStakeAccountUpdate;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.cardanofoundation.rewards.enums.AccountUpdateAction.fromString;
import static org.cardanofoundation.rewards.util.BigNumberUtils.*;
import static org.cardanofoundation.rewards.util.BigNumberUtils.divide;

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
    public static BigDecimal calculateApparentPoolPerformance(final BigInteger activePoolStake, final BigInteger totalActiveEpochStake, final int blocksMintedByPool, final int blocksMintedByStakePools, final double decentralizationParam) {
        BigDecimal poolStake = new BigDecimal(activePoolStake);
        BigDecimal totalEpochStake = new BigDecimal(totalActiveEpochStake);

        if (decentralizationParam >= 0.8) {
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
    public static BigInteger calculateOptimalPoolReward(BigInteger totalAvailableRewards, int optimalPoolCount, double influence, BigDecimal relativeStakeOfPool, BigDecimal relativeStakeOfPoolOwner) {

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
    public static BigInteger calculateLeaderReward(BigInteger poolReward, double margin, BigInteger poolCost,
                                                   BigDecimal relativeOwnerStake, BigDecimal relativeStakeOfPool) {
        if (isLowerOrEquals(poolReward, poolCost)) {
            return poolReward;
        }

        return add(new BigDecimal(poolCost).toBigInteger(), floor(multiply(subtract(poolReward, poolCost),
                add(margin, multiply((1 - margin), divide(relativeOwnerStake, relativeStakeOfPool))))));
    }

    /*
     * This method calculates the pool member reward regarding the formula described
     * in the shelly-ledger.pdf p. 61, figure 47
     *
     * See Haskell implementation: https://github.com/input-output-hk/cardano-ledger/blob/aed5dde9cd1096cfc2e255879cd617c0d64f8d9d/eras/shelley/impl/src/Cardano/Ledger/Shelley/Rewards.hs#L117
     */
    public static BigInteger calculateMemberReward(BigInteger poolReward, double margin, BigInteger poolCost,
                                                   BigDecimal relativeMemberStake, BigDecimal relativeStakeOfPool) {
        if (isLowerOrEquals(poolReward, poolCost)) {
            return BigInteger.ZERO;
        }

        return floor(divide(multiply(
                poolReward.subtract(poolCost),
                subtract(BigDecimal.ONE, margin),
                relativeMemberStake), relativeStakeOfPool));
    }

    public static PoolRewardCalculationResult calculatePoolRewardInEpoch(String poolId, PoolHistory poolHistoryCurrentEpoch,
                                                                         int totalBlocksInEpoch, ProtocolParameters protocolParameters,
                                                                         BigInteger adaInCirculation, BigInteger activeStakeInEpoch, BigInteger stakePoolRewardsPot,
                                                                         BigInteger totalActiveStakeOfOwners, List<String> poolOwnerStakeAddresses,
                                                                         List<LatestStakeAccountUpdate> accountUpdates, BigInteger poolOperatorRewardOutlier) {
        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210
        PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculationResult.builder()
                .epoch(poolHistoryCurrentEpoch.getEpoch())
                .poolId(poolId)
                .poolReward(BigInteger.ZERO)
                .distributedPoolReward(BigInteger.ZERO)
                .build();

        BigInteger poolStake = poolHistoryCurrentEpoch.getActiveStake();
        BigInteger poolPledge = poolHistoryCurrentEpoch.getPledge();
        double poolMargin = poolHistoryCurrentEpoch.getMargin();
        BigInteger poolFixedCost = poolHistoryCurrentEpoch.getFixedCost();
        int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCount();

        poolRewardCalculationResult.setPoolMargin(poolMargin);
        poolRewardCalculationResult.setPoolCost(poolFixedCost);
        poolRewardCalculationResult.setRewardAddress(poolHistoryCurrentEpoch.getRewardAddress());

        if (blocksPoolHasMinted == 0) {
            return poolRewardCalculationResult;
        }

        double decentralizationParameter = protocolParameters.getDecentralisation();
        int optimalPoolCount = protocolParameters.getOptimalPoolCount();
        double influenceParam = protocolParameters.getPoolOwnerInfluence();

        // Step 5: Calculate apparent pool performance
        BigDecimal apparentPoolPerformance =
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

        BigDecimal relativeStakeOfPoolOwner = divide(poolPledge, adaInCirculation);
        BigDecimal relativePoolStake = divide(poolStake, adaInCirculation);

        // Step 8: Calculate optimal pool reward
        BigInteger optimalPoolReward =
                PoolRewardsCalculation.calculateOptimalPoolReward(
                        stakePoolRewardsPot,
                        optimalPoolCount,
                        influenceParam,
                        relativePoolStake,
                        relativeStakeOfPoolOwner);
        poolRewardCalculationResult.setOptimalPoolReward(optimalPoolReward);

        // Step 9: Calculate pool reward as optimal pool reward * apparent pool performance
        BigInteger poolReward = PoolRewardsCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);
        poolRewardCalculationResult.setPoolReward(poolReward);

        // Step 10: Calculate pool operator reward
        BigInteger poolOperatorReward = PoolRewardsCalculation.calculateLeaderReward(poolReward, poolMargin, poolFixedCost,
                divide(totalActiveStakeOfOwners, adaInCirculation), relativePoolStake);

        Map<String, AccountUpdate> latestAccountUpdates = new HashMap<>();
        for (LatestStakeAccountUpdate accountUpdate : accountUpdates) {
            if (latestAccountUpdates.keySet().stream().noneMatch(stakeAddress -> stakeAddress.equals(accountUpdate.getStakeAddress()))) {
                latestAccountUpdates.put(accountUpdate.getStakeAddress(),
                        AccountUpdate.builder()
                                .stakeAddress(accountUpdate.getStakeAddress())
                                .action(AccountUpdateAction.fromString(accountUpdate.getLatestUpdateType()))
                                .build());
            }
        }

        if (accountUpdates.size() > 0 &&
                (latestAccountUpdates.get(poolRewardCalculationResult.getRewardAddress()) == null ||
                        latestAccountUpdates.get(poolRewardCalculationResult.getRewardAddress()).getAction().equals(AccountUpdateAction.DEREGISTRATION))) {
            poolOperatorReward = BigInteger.ZERO;
        }

        poolOperatorReward = poolOperatorReward.add(poolOperatorRewardOutlier);
        poolRewardCalculationResult.setOperatorReward(poolOperatorReward);
        poolRewardCalculationResult.setDistributedPoolReward(poolOperatorReward);
        // Step 11: Calculate pool member reward
        List<Reward> memberRewards = new ArrayList<>();
        for (Delegator delegator : poolHistoryCurrentEpoch.getDelegators()) {
            if (accountUpdates.size() > 0 &&
                    (latestAccountUpdates.get(delegator.getStakeAddress()) == null ||
                            latestAccountUpdates.get(delegator.getStakeAddress()).getAction().equals(AccountUpdateAction.DEREGISTRATION))) {
                continue;
            }

            if (delegator.getStakeAddress().equals(poolHistoryCurrentEpoch.getRewardAddress()) ||
                    poolOwnerStakeAddresses.contains(delegator.getStakeAddress())) {
                continue;
            }

            BigInteger memberReward = PoolRewardsCalculation.calculateMemberReward(poolReward, poolMargin,
                    poolFixedCost, divide(delegator.getActiveStake(), adaInCirculation), relativePoolStake);
            memberRewards.add(Reward.builder()
                    .amount(memberReward)
                    .stakeAddress(delegator.getStakeAddress())
                    .build());

            poolRewardCalculationResult.setDistributedPoolReward(
                    add(poolRewardCalculationResult.getDistributedPoolReward(), memberReward));
        }
        poolRewardCalculationResult.setMemberRewards(memberRewards);
        return poolRewardCalculationResult;
    }
}
