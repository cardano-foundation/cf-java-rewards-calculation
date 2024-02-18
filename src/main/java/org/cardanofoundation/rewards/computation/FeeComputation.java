package org.cardanofoundation.rewards.computation;

import org.cardanofoundation.rewards.data.provider.DataProvider;

import java.math.BigInteger;

public class FeeComputation {

    public static BigInteger calculateFeePotInEpoch(int epoch, DataProvider dataProvider) {
        BigInteger fees = BigInteger.ZERO;

        if (epoch > 208) {
            fees = dataProvider.getSumOfFeesInEpoch(epoch - 1);
        }

        return fees;
    }
}
