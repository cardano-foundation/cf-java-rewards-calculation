package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;

public class FeeCalculation {

    public static double calculateFeePotInEpoch(int epoch, DataProvider dataProvider) {
        double fees = 0;

        if (epoch > 208) {
            fees = dataProvider.getSumOfFeesInEpoch(epoch - 1);
        }

        return fees;
    }
}
