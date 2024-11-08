package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.calculation.domain.RetiredPool;

import java.math.BigInteger;
import java.util.Set;

public class DepositsCalculation {

    public static BigInteger calculateDepositsInEpoch(BigInteger depositsInPreviousEpoch,
                                                      BigInteger transactionDepositsInEpoch,
                                                      Set<RetiredPool> retiredPoolsInEpoch) {
        BigInteger deposits = BigInteger.ZERO;
        deposits = deposits.add(transactionDepositsInEpoch);

        //add deposits of retired pools
        var refundAmount = retiredPoolsInEpoch.stream()
                .reduce(BigInteger.ZERO, (sum, retiredPool) -> sum.add(retiredPool.getDepositAmount()), BigInteger::add);

        deposits = deposits.subtract(refundAmount);
        return depositsInPreviousEpoch.add(deposits);
    }
}
