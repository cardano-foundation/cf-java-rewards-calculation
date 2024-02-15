package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.cardanofoundation.rewards.constants.RewardConstants.TOTAL_LOVELACE;
import static org.cardanofoundation.rewards.util.BigNumberUtils.*;

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
    public static BigInteger calculateLeaderReward(BigInteger poolReward, double margin, double poolCost,
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
    public static BigInteger calculateMemberReward(BigInteger poolReward, double margin, double poolCost,
                                               BigDecimal relativeMemberStake, BigDecimal relativeStakeOfPool) {
        if (isLowerOrEquals(poolReward, poolCost)) {
            return BigInteger.ZERO;
        }

        return floor(divide(multiply(
                subtract(poolReward, poolCost),
                subtract(BigDecimal.ONE, margin),
                relativeMemberStake), relativeStakeOfPool));
    }

    public static PoolRewardCalculationResult calculatePoolRewardInEpoch(String poolId, int epoch, DataProvider dataProvider) {
        // Step 1: Get Pool information of current epoch
        // Example: https://api.koios.rest/api/v0/pool_history?_pool_bech32=pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt&_epoch_no=210
        PoolRewardCalculationResult poolRewardCalculationResult = PoolRewardCalculationResult.builder()
                .epoch(epoch)
                .poolId(poolId)
                .poolReward(BigInteger.ZERO)
                .build();

        PoolHistory poolHistoryCurrentEpoch = dataProvider.getPoolHistory(poolId, epoch);
        if(poolHistoryCurrentEpoch == null) {
            return poolRewardCalculationResult;
        }

        BigInteger poolStake = poolHistoryCurrentEpoch.getActiveStake();
        BigInteger poolFees = poolHistoryCurrentEpoch.getPoolFees();
        double poolMargin = poolHistoryCurrentEpoch.getMargin();
        double poolFixedCost = poolHistoryCurrentEpoch.getFixedCost();
        int blocksPoolHasMinted = poolHistoryCurrentEpoch.getBlockCount();

        poolRewardCalculationResult.setPoolFee(poolFees);
        poolRewardCalculationResult.setPoolMargin(poolMargin);
        poolRewardCalculationResult.setPoolCost(poolFixedCost);
        poolRewardCalculationResult.setRewardAddress(poolHistoryCurrentEpoch.getRewardAddress());

        if (blocksPoolHasMinted == 0) {
            return poolRewardCalculationResult;
        }

        // Step 2: Get Epoch information of current epoch
        // Source: https://api.koios.rest/api/v0/epoch_info?_epoch_no=211
        Epoch epochInfo = dataProvider.getEpochInfo(epoch);

        BigInteger activeStakeInEpoch = BigInteger.ZERO;
        if (epochInfo.getActiveStake() != null) {
            activeStakeInEpoch = epochInfo.getActiveStake();
        }

        // The Shelley era and the ada pot system started on mainnet in epoch 208.
        // Fee and treasury values are 0 for epoch 208.
        BigInteger totalFeesForCurrentEpoch = BigInteger.ZERO;
        if (epoch > 209) {
            totalFeesForCurrentEpoch = epochInfo.getFees();
        }

        int totalBlocksInEpoch = epochInfo.getBlockCount();

        if (epoch > 212 && epoch < 255) {
            totalBlocksInEpoch = epochInfo.getNonOBFTBlockCount();
        }

        // Get the ada reserves for the next epoch because it was already updated (int the previous step)
        AdaPots adaPotsForNextEpoch = dataProvider.getAdaPotsForEpoch(epoch + 1);
        BigInteger reserves = adaPotsForNextEpoch.getReserves();

        // Step 3: Get total ada in circulation
        BigInteger adaInCirculation = TOTAL_LOVELACE.subtract(reserves);

        // Step 4: Get protocol parameters for current epoch
        ProtocolParameters protocolParameters = dataProvider.getProtocolParametersForEpoch(epoch);
        double decentralizationParameter = protocolParameters.getDecentralisation();
        int optimalPoolCount = protocolParameters.getOptimalPoolCount();
        double influenceParam = protocolParameters.getPoolOwnerInfluence();
        double monetaryExpandRate = protocolParameters.getMonetaryExpandRate();
        double treasuryGrowRate = protocolParameters.getTreasuryGrowRate();

        // Step 5: Calculate apparent pool performance
        BigDecimal apparentPoolPerformance =
                PoolRewardCalculation.calculateApparentPoolPerformance(poolStake, activeStakeInEpoch,
                        blocksPoolHasMinted, totalBlocksInEpoch, decentralizationParameter);
        poolRewardCalculationResult.setApparentPoolPerformance(apparentPoolPerformance);
        // Step 6: Calculate total available reward for pools (total reward pot after treasury cut)
        // -----
        BigInteger totalRewardPot = TreasuryCalculation.calculateTotalRewardPotWithEta(
                monetaryExpandRate, totalBlocksInEpoch, decentralizationParameter, reserves, totalFeesForCurrentEpoch);

        BigInteger stakePoolRewardsPot = totalRewardPot.subtract(floor(multiply(totalRewardPot, treasuryGrowRate)));
        poolRewardCalculationResult.setStakePoolRewardsPot(stakePoolRewardsPot);
        // shelley-delegation.pdf 5.5.3
        //      "[...]the relative stake of the pool owner(s) (the amount of ada
        //      pledged during pool registration)"

        // Step 7: Get the latest pool update before this epoch and extract the pledge
        double poolPledge = dataProvider.getPoolPledgeInEpoch(poolId, epoch);

        PoolOwnerHistory poolOwnersHistoryInEpoch = dataProvider.getHistoryOfPoolOwnersInEpoch(poolId, epoch);
        BigInteger totalActiveStakeOfOwners = poolOwnersHistoryInEpoch.getActiveStake();
        poolRewardCalculationResult.setPoolOwnerStakeAddresses(poolOwnersHistoryInEpoch.getStakeAddresses());

        if (isLower(totalActiveStakeOfOwners, poolPledge)) {
            return poolRewardCalculationResult;
        }

        BigDecimal relativeStakeOfPoolOwner = divide(poolPledge, adaInCirculation);
        BigDecimal relativePoolStake = divide(poolStake, adaInCirculation);

        // Step 8: Calculate optimal pool reward
        BigInteger optimalPoolReward =
                PoolRewardCalculation.calculateOptimalPoolReward(
                        stakePoolRewardsPot,
                        optimalPoolCount,
                        influenceParam,
                        relativePoolStake,
                        relativeStakeOfPoolOwner);
        poolRewardCalculationResult.setOptimalPoolReward(optimalPoolReward);

        // Step 9: Calculate pool reward as optimal pool reward * apparent pool performance
        BigInteger poolReward = PoolRewardCalculation.calculatePoolReward(optimalPoolReward, apparentPoolPerformance);
        poolRewardCalculationResult.setPoolReward(poolReward);

        // Step 10: Calculate pool operator reward
        BigInteger poolOperatorReward = PoolRewardCalculation.calculateLeaderReward(poolReward, poolMargin, poolFixedCost,
                divide(totalActiveStakeOfOwners, adaInCirculation), relativePoolStake);

        // Step 10 a: Check if pool reward address has been unregistered before
        List<AccountUpdate> accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(List.of(poolRewardCalculationResult.getRewardAddress()), epoch);
        accountUpdates = accountUpdates.stream().filter(update ->
                update.getAction().equals(AccountUpdateAction.DEREGISTRATION)
                        || update.getAction().equals(AccountUpdateAction.REGISTRATION)).sorted(
                Comparator.comparing(AccountUpdate::getUnixBlockTime).reversed()).toList();

        if (accountUpdates.size() > 0 && accountUpdates.get(0).getAction().equals(AccountUpdateAction.DEREGISTRATION)) {
            poolOperatorReward = BigInteger.ZERO;
        }

        poolOperatorReward = poolOperatorReward.add(correctOutliers(poolId, epoch + 2));

        poolRewardCalculationResult.setOperatorReward(poolOperatorReward);
        // Step 11: Calculate pool member reward
        List<Reward> memberRewards = new ArrayList<>();
        for (Delegator delegator : poolHistoryCurrentEpoch.getDelegators()) {
            accountUpdates = dataProvider.getAccountUpdatesUntilEpoch(List.of(delegator.getStakeAddress()), epoch);
            if (accountUpdates.size() > 0 && accountUpdates.get(0).getAction().equals(AccountUpdateAction.DEREGISTRATION)) {
                continue;
            }

            BigInteger memberReward = PoolRewardCalculation.calculateMemberReward(poolReward, poolMargin,
                    poolFixedCost, divide(delegator.getActiveStake(), adaInCirculation), relativePoolStake);
            memberRewards.add(Reward.builder()
                    .amount(memberReward)
                    .stakeAddress(delegator.getStakeAddress())
                    .build());
        }
        poolRewardCalculationResult.setMemberRewards(memberRewards);
        return poolRewardCalculationResult;
    }

    /*
        TODO:   Replace this method with the jpa repository call to find reward address owning
                multiple pools that produced blocks in the same epoch
     */
    public static BigInteger correctOutliers(String poolId, int epoch) {
        BigInteger correction = BigInteger.ZERO;

        if (epoch == 214 && poolId.equals("pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m")) {
            /*
             * The reward_address of pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m is also the
             * reward_address of pool1gh4cj5h5glk5992d0wtela324htr0cn8ujvg53pmuds9guxgz2u. Both pools produced
             * blocks in epoch 214. In a previous node version this caused an outlier where the
             * leader rewards of pool13l0j202yexqh6l0awtee9g354244gmfze09utxz0sn7p7r3ev3m has been set to 0.
             *
             * This behavior has been changed later so that the owner would receive leader rewards for both pools.
             * Affected reward addresses have been paid out due to a MIR certificate afterward.
             */
            correction = new BigInteger(String.valueOf(-814592210));
        } else if (epoch == 214 && poolId.equals("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")) {
            // pool1qvvn2l690zm3v2p0f3vd66ly6cfs2wjqx34zpqcx5pwsx3eprtp also produced blocks in epoch 214
            // with the same reward address
            correction = new BigInteger(String.valueOf(-669930045));
        } else if (epoch == 215 && poolId.equals("pool166dkk9kx5y6ug9tnvh0dnvxhwt2yca3g5pd5jaqa8t39cgyqqlr")) {
            // pool1qvvn2l690zm3v2p0f3vd66ly6cfs2wjqx34zpqcx5pwsx3eprtp also produced blocks in epoch 215
            // with the same reward address
            correction = new BigInteger(String.valueOf(-634057195));
        } else if (epoch == 215 && poolId.equals("pool17rns3wjyql9jg9xkzw9h88f0kstd693pm6urwxmvejqgsyjw7ta")) {
            // pool12crd62rxj8yryvshmgwkxza7um3uhaypwdjeel98lnkf529qdw5 &
            // pool1v4adhelnswa7pwv2njn5h84atw08mlc79ll2ewl2kgxhv3cqwql also produced blocks in epoch 215
            // with the same reward address
            correction = new BigInteger(String.valueOf(-369216375.99999997));
        }

        return correction;
    }
}
