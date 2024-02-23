package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.validation.data.provider.DataProvider;

import java.math.BigInteger;

public class FeeValidation {

    public static BigInteger calculateFeePotInEpoch(int epoch, DataProvider dataProvider) {
        BigInteger fees = BigInteger.ZERO;

        if (epoch > 208) {
            fees = dataProvider.getSumOfFeesInEpoch(epoch - 1);
        }

        return fees;
    }
}
