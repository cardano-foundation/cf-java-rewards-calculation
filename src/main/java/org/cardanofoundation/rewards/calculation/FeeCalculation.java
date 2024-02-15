package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;

import java.math.BigInteger;

public class FeeCalculation {

    public static BigInteger calculateFeePotInEpoch(int epoch, DataProvider dataProvider) {
        BigInteger fees = BigInteger.ZERO;

        if (epoch > 208) {
            fees = dataProvider.getSumOfFeesInEpoch(epoch - 1);
        }

        return fees;
    }
}
