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
            int actualPoolDeregistrationsInEpoch = 0;

            for (PoolDeregistration poolDeregistration : retiredPoolsInEpoch) {
                boolean poolDeregistrationLaterInEpoch = retiredPoolsInEpoch.stream().anyMatch(
                        deregistration -> deregistration.getPoolId().equals(poolDeregistration.getPoolId()) &&
                                deregistration.getAnnouncedTransactionId() > poolDeregistration.getAnnouncedTransactionId()
                );

                // To prevent double counting, we only count the pool deregistration if there is no other deregistration
                // for the same pool later in the epoch
                if (poolDeregistrationLaterInEpoch) {
                    continue;
                }

                List<PoolUpdate> poolUpdates = dataProvider.getPoolUpdateAfterTransactionIdInEpoch(poolDeregistration.getPoolId(),
                        poolDeregistration.getAnnouncedTransactionId(), epoch);

                // There is an update after the deregistration, so the pool was not retired
                if (poolUpdates.size() == 0) {
                    PoolDeregistration latestPoolRetirementUntilEpoch = dataProvider.latestPoolRetirementUntilEpoch(poolDeregistration.getPoolId(), epoch);
                    if (latestPoolRetirementUntilEpoch != null && latestPoolRetirementUntilEpoch.getRetiringEpoch() != epoch + 1) {
                        // The pool was retired in a previous epoch for the next epoch, but another deregistration was announced and changed the
                        // retirement epoch to something else. This means the pool was not retired in this epoch.
                        continue;
                    }

                    actualPoolDeregistrationsInEpoch += 1;
                }
            }

            deposit += transactionDepositsInEpoch;
            deposit -= actualPoolDeregistrationsInEpoch * DEPOSIT_POOL_REGISTRATION_IN_LOVELACE;
        }

        return depositsInPreviousEpoch + deposit;
    }
}
