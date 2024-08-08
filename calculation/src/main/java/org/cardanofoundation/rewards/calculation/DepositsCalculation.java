package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;

import java.math.BigInteger;
import static org.cardanofoundation.rewards.calculation.util.BigNumberUtils.multiply;

public class DepositsCalculation {

    public static BigInteger calculateDepositsInEpoch(BigInteger depositsInPreviousEpoch,
                                                      BigInteger transactionDepositsInEpoch,
                                                      int retiredPoolsInEpoch,
                                                      NetworkConfig networkConfig) {
        BigInteger deposits = BigInteger.ZERO;
        deposits = deposits.add(transactionDepositsInEpoch);
        deposits = deposits.subtract(
                    multiply(retiredPoolsInEpoch, networkConfig.getPoolDepositInLovelace()));
        return depositsInPreviousEpoch.add(deposits);
    }
}
