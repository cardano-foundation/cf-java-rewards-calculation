package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;
import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.PoolDeregistration;

import java.math.BigInteger;
import java.util.List;

import static org.cardanofoundation.rewards.constants.RewardConstants.POOL_DEPOSIT_IN_LOVELACE;
import static org.cardanofoundation.rewards.util.BigNumberUtils.multiply;

public class DepositCalculation {

    public static BigInteger calculateDepositInEpoch(int epoch, DataProvider dataProvider) {
        AdaPots adaPots = dataProvider.getAdaPotsForEpoch(epoch);
        BigInteger depositsInPreviousEpoch = adaPots.getDeposits();
        BigInteger deposit = BigInteger.ZERO;

        if (epoch > 207) {
            BigInteger transactionDepositsInEpoch = dataProvider.getTransactionDepositsInEpoch(epoch);
            List<PoolDeregistration> retiredPoolsInEpoch = dataProvider.getRetiredPoolsInEpoch(epoch + 1);

            deposit = deposit.add(transactionDepositsInEpoch);
            deposit = deposit.subtract(
                    multiply(retiredPoolsInEpoch.size(), POOL_DEPOSIT_IN_LOVELACE));
        }

        return depositsInPreviousEpoch.add(deposit);
    }
}
