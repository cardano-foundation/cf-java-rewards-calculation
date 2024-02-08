package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.PoolDeregistration;
import org.cardanofoundation.rewards.entity.PoolUpdate;

import java.util.List;

import static org.cardanofoundation.rewards.constants.RewardConstants.DEPOSIT_POOL_REGISTRATION_IN_LOVELACE;

public class DepositCalculation {

    public static double calculateDepositInEpoch(int epoch, DataProvider dataProvider) {
        AdaPots adaPots = dataProvider.getAdaPotsForEpoch(epoch);
        double depositsInPreviousEpoch = adaPots.getDeposits();
        double deposit = 0;

        if (epoch > 207) {
            double transactionDepositsInEpoch = dataProvider.getTransactionDepositsInEpoch(epoch);
            List<PoolDeregistration> retiredPoolsInEpoch = dataProvider.getRetiredPoolsInEpoch(epoch + 1);

            deposit += transactionDepositsInEpoch;
            deposit -= retiredPoolsInEpoch.size() * DEPOSIT_POOL_REGISTRATION_IN_LOVELACE;
        }

        return depositsInPreviousEpoch + deposit;
    }
}
