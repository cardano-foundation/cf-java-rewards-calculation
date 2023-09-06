package org.cardanofoundation.rewards.util;

import java.math.BigDecimal;

public class CurrencyConverter {

    public static final double ADA_TO_LOVELACE = 1000000.0;
    public static final double LOVELACE_TO_ADA = 1.0 / ADA_TO_LOVELACE;

    public static double lovelaceToAda(double lovelace) {
        return lovelace * LOVELACE_TO_ADA;
    }

    public static double adaToLovelace(double ada) {
        return ada * ADA_TO_LOVELACE;
    }

    public static BigDecimal lovelaceToAda(BigDecimal lovelace) {
        return lovelace.multiply(BigDecimal.valueOf(LOVELACE_TO_ADA));
    }

    public static BigDecimal adaToLovelace(BigDecimal ada) {
        return ada.multiply(BigDecimal.valueOf(ADA_TO_LOVELACE));
    }
}
