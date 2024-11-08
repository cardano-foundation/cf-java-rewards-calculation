package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.RetiredPool;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.calculation.domain.AdaPots;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.cardanofoundation.rewards.calculation.DepositsCalculation.calculateDepositsInEpoch;

public class DepositsValidation {

    public static BigInteger computeDepositsInEpoch(int epoch, DataProvider dataProvider, NetworkConfig networkConfig) {
        AdaPots adaPots = dataProvider.getAdaPotsForEpoch(epoch);
        BigInteger depositsInPreviousEpoch = adaPots.getDeposits();
        BigInteger transactionDepositsInEpoch = BigInteger.ZERO;
        Set<RetiredPool> retiredPoolsInEpoch = new HashSet<>();

        if (epoch >= networkConfig.getShelleyStartEpoch()) {
            transactionDepositsInEpoch = dataProvider.getTransactionDepositsInEpoch(epoch);
            retiredPoolsInEpoch = dataProvider.getRetiredPoolsInEpoch(epoch + 1);
        }

        return calculateDepositsInEpoch(depositsInPreviousEpoch,
                transactionDepositsInEpoch, retiredPoolsInEpoch);
    }
}
