package org.cardanofoundation.rewards.util;

public class CurrencyConverter {
    public static final double ADA_TO_LOVELACE = 1000000.0;
    public static final double LOVELACE_TO_ADA = 1.0 / ADA_TO_LOVELACE;

    public static double lovelaceToAda(double lovelace) {
        return lovelace * LOVELACE_TO_ADA;
    }

    public static double adaToLovelace(double ada) {
        return ada * ADA_TO_LOVELACE;
    }
}
