package org.cardanofoundation.rewards.computation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.PoolDeregistration;

import java.math.BigInteger;
import java.util.List;

import static org.cardanofoundation.rewards.calculation.DepositsCalculation.calculateDepositsInEpoch;
import static org.cardanofoundation.rewards.util.BigNumberUtils.multiply;

public class DepositsComputation {

    public static BigInteger computeDepositsInEpoch(int epoch, DataProvider dataProvider) {
        AdaPots adaPots = dataProvider.getAdaPotsForEpoch(epoch);
        BigInteger depositsInPreviousEpoch = adaPots.getDeposits();
        BigInteger transactionDepositsInEpoch = BigInteger.ZERO;
        List<PoolDeregistration> retiredPoolsInEpoch = List.of();

        if (epoch > 207) {
            transactionDepositsInEpoch = dataProvider.getTransactionDepositsInEpoch(epoch);
            retiredPoolsInEpoch = dataProvider.getRetiredPoolsInEpoch(epoch + 1);
        }

        return calculateDepositsInEpoch(depositsInPreviousEpoch,
                transactionDepositsInEpoch, retiredPoolsInEpoch.size());
    }
}
