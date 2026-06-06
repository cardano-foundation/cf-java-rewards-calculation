package org.cardanofoundation.rewards.calculation.util;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Exact rational arithmetic for ledger calculations that need Haskell
 * {@code Rational}-style behavior and a single explicit floor at the end.
 */
public final class Ratio {
    public static final Ratio ZERO = new Ratio(BigInteger.ZERO, BigInteger.ONE);
    public static final Ratio ONE = new Ratio(BigInteger.ONE, BigInteger.ONE);

    private final BigInteger numerator;
    private final BigInteger denominator;

    private Ratio(BigInteger numerator, BigInteger denominator) {
        if (denominator.signum() == 0) {
            throw new IllegalArgumentException("Ratio denominator must not be zero");
        }
        if (numerator.signum() == 0) {
            this.numerator = BigInteger.ZERO;
            this.denominator = BigInteger.ONE;
            return;
        }

        BigInteger normalizedNumerator = numerator;
        BigInteger normalizedDenominator = denominator;
        if (normalizedDenominator.signum() < 0) {
            normalizedNumerator = normalizedNumerator.negate();
            normalizedDenominator = normalizedDenominator.negate();
        }

        BigInteger gcd = normalizedNumerator.gcd(normalizedDenominator);
        this.numerator = normalizedNumerator.divide(gcd);
        this.denominator = normalizedDenominator.divide(gcd);
    }

    public static Ratio of(BigInteger numerator) {
        return of(numerator, BigInteger.ONE);
    }

    public static Ratio of(BigInteger numerator, BigInteger denominator) {
        return new Ratio(numerator, denominator);
    }

    public static Ratio from(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        BigInteger numerator = normalized.unscaledValue();
        int scale = normalized.scale();
        if (scale < 0) {
            return of(numerator.multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE);
        }
        return of(numerator, BigInteger.TEN.pow(scale));
    }

    public BigInteger numerator() {
        return numerator;
    }

    public BigInteger denominator() {
        return denominator;
    }

    public Ratio multiply(Ratio other) {
        return of(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
    }

    public Ratio subtract(Ratio other) {
        return of(numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator)),
                denominator.multiply(other.denominator));
    }

    public BigInteger floor() {
        return floor(numerator, denominator);
    }

    public BigInteger multiplyAndFloor(BigInteger value) {
        return floor(value.multiply(numerator), denominator);
    }

    @Override
    public String toString() {
        return numerator + "/" + denominator;
    }

    private static BigInteger floor(BigInteger numerator, BigInteger denominator) {
        BigInteger[] quotientAndRemainder = numerator.divideAndRemainder(denominator);
        if (numerator.signum() < 0 && quotientAndRemainder[1].signum() != 0) {
            return quotientAndRemainder[0].subtract(BigInteger.ONE);
        }
        return quotientAndRemainder[0];
    }
}
