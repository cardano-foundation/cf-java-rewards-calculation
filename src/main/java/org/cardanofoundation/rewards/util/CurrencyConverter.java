package org.cardanofoundation.rewards.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class CurrencyConverter {

    public static final MathContext FLOOR_MATH_CONTEXT = new MathContext(0, RoundingMode.FLOOR);
    public static final MathContext HALF_UP_MATH_CONTEXT = new MathContext(30, RoundingMode.HALF_UP);
    public static final BigDecimal ADA_TO_LOVELACE = new BigDecimal("1000000.0", HALF_UP_MATH_CONTEXT);
    public static final BigDecimal LOVELACE_TO_ADA = BigDecimal.ONE.divide(ADA_TO_LOVELACE, HALF_UP_MATH_CONTEXT);

    public static BigDecimal lovelaceToAda(BigDecimal lovelace) {
        return lovelace.multiply(LOVELACE_TO_ADA, HALF_UP_MATH_CONTEXT);
    }

    public static BigDecimal adaToLovelace(final BigDecimal ada) {
        return ada.multiply(ADA_TO_LOVELACE, FLOOR_MATH_CONTEXT);
    }
}
