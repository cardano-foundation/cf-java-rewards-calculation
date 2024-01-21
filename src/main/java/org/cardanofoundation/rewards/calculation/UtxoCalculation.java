package org.cardanofoundation.rewards.calculation;

import org.cardanofoundation.rewards.data.provider.DataProvider;

public class UtxoCalculation {

    public final static double UTXO_POT_BEGINNING_OF_SHELLY = 31111977147073356.0;

    public static double calculateUtxoPotInEpoch(int epoch, DataProvider dataProvider) {
        double utxo = UTXO_POT_BEGINNING_OF_SHELLY;

        if (epoch > 208) {
            double utxoFromPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1).getAdaInCirculation();
            double deposits = dataProvider.getTransactionDepositsInEpoch(epoch - 1);
            double fees = dataProvider.getSumOfFeesInEpoch(epoch - 1);
            double withdrawals = 0.0;

            if (epoch > 209) {
                withdrawals = dataProvider.getSumOfWithdrawalsInEpoch(epoch - 1);
            }

            utxo = utxoFromPreviousEpoch - deposits - fees + withdrawals;

            if (epoch == 236) {
                // Todo: verify with yaci-store data provider later
                /*
                    "The bootstrap addresses from Figure 6 were not intended to include the Byron era redeem
                    addresses (those with addrtype 2, see the Byron CDDL spec). These addresses were, however,
                    not spendable in the Shelley era. At the Allegra hard fork they were removed from the UTxO
                    and the Ada contained in them was returned to the reserves."
                        - shelley-spec-ledger.pdf 17.5 p.115
                 */
                utxo = utxo - 318200635000000.0;
            }
        }

        return utxo;
    }
}
