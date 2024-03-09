package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.calculation.domain.AdaPots;
import java.math.BigInteger;
import java.util.HashSet;

import static org.cardanofoundation.rewards.calculation.DepositsCalculation.calculateDepositsInEpoch;

public class DepositsValidation {

    public static BigInteger computeDepositsInEpoch(int epoch, DataProvider dataProvider) {
        AdaPots adaPots = dataProvider.getAdaPotsForEpoch(epoch);
        BigInteger depositsInPreviousEpoch = adaPots.getDeposits();
        BigInteger transactionDepositsInEpoch = BigInteger.ZERO;
        HashSet<String> retiredPoolsInEpoch = new HashSet<>();

        if (epoch > 207) {
            transactionDepositsInEpoch = dataProvider.getTransactionDepositsInEpoch(epoch);
            retiredPoolsInEpoch = dataProvider.getRewardAddressesOfRetiredPoolsInEpoch(epoch + 1);
        }

        return calculateDepositsInEpoch(depositsInPreviousEpoch,
                transactionDepositsInEpoch, retiredPoolsInEpoch.size());
    }
}
