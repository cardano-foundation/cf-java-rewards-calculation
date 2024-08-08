package org.cardanofoundation.rewards.validation;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;

import java.math.BigInteger;

public class UtxoValidation {

    public final static BigInteger UTXO_POT_BEGINNING_OF_SHELLY = BigInteger.valueOf(31111977147073356L);

    public static BigInteger calculateUtxoPotInEpoch(int epoch, DataProvider dataProvider, NetworkConfig networkConfig) {
        BigInteger utxo = UTXO_POT_BEGINNING_OF_SHELLY;

        if (epoch > 208) {
            BigInteger utxoFromPreviousEpoch = dataProvider.getAdaPotsForEpoch(epoch - 1).getAdaInCirculation();
            BigInteger deposits = dataProvider.getTransactionDepositsInEpoch(epoch - 1);
            BigInteger fees = dataProvider.getSumOfFeesInEpoch(epoch - 1);
            BigInteger withdrawals = BigInteger.ZERO;

            if (epoch > 209) {
                withdrawals = dataProvider.getSumOfWithdrawalsInEpoch(epoch - 1);
            }

            utxo = utxoFromPreviousEpoch.subtract(deposits).subtract(fees).add(withdrawals);

            if (epoch == networkConfig.getMainnetAllegraHardforkEpoch()) {
                // Todo: verify with yaci-store data provider later
                /*
                    "The bootstrap addresses from Figure 6 were not intended to include the Byron era redeem
                    addresses (those with addrtype 2, see the Byron CDDL spec). These addresses were, however,
                    not spendable in the Shelley era. At the Allegra hard fork they were removed from the UTxO
                    and the Ada contained in them was returned to the reserves."
                        - shelley-spec-ledger.pdf 17.5 p.115
                 */
                utxo = utxo.subtract(networkConfig.getMainnetBootstrapAddressAmount());
            }
        }

        return utxo;
    }
}
