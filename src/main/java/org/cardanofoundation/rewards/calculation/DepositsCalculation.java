package org.cardanofoundation.rewards.calculation;

import java.math.BigInteger;
import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.POOL_DEPOSIT_IN_LOVELACE;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.multiply;

public class DepositsCalculation {

    public static BigInteger calculateDepositsInEpoch(BigInteger depositsInPreviousEpoch,
                                                      BigInteger transactionDepositsInEpoch,
                                                      int retiredPoolsInEpoch) {
        BigInteger deposits = BigInteger.ZERO;
        deposits = deposits.add(transactionDepositsInEpoch);
        deposits = deposits.subtract(
                    multiply(retiredPoolsInEpoch, POOL_DEPOSIT_IN_LOVELACE));
        return depositsInPreviousEpoch.add(deposits);
    }
}
