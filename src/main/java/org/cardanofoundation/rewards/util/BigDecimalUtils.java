package org.cardanofoundation.rewards.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class BigDecimalUtils {
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b);
    }

    public static BigDecimal add(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).add(b);
    }

    public static BigDecimal add(BigDecimal a, double b) {
        return a.add(BigDecimal.valueOf(b));
    }

    public static BigDecimal add(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b));
    }

    public static BigDecimal add(long a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b));
    }

    public static BigDecimal add(double a, long b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b);
    }

    public static BigDecimal subtract(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).subtract(b);
    }

    public static BigDecimal subtract(BigDecimal a, double b) {
        return a.subtract(BigDecimal.valueOf(b));
    }

    public static BigDecimal subtract(double a, double b) {
        return BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b));
    }

    public static BigDecimal subtract(long a, double b) {
        return BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b));
    }

    public static BigDecimal subtract(double a, long b) {
        return BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b));
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b);
    }

    public static BigDecimal multiply(BigDecimal a, BigDecimal b, BigDecimal c) {
        return a.multiply(b).multiply(c);
    }

    public static BigDecimal multiply(BigDecimal a, double b, BigDecimal c) {
        return a.multiply(BigDecimal.valueOf(b)).multiply(c);
    }

    public static BigDecimal multiply(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).multiply(b);
    }

    public static BigDecimal multiply(BigDecimal a, double b) {
        return a.multiply(BigDecimal.valueOf(b));
    }

    public static BigDecimal multiply(double a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b));
    }

    public static BigDecimal multiply(long a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b));
    }

    public static BigDecimal multiply(double a, long b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b));
    }

    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, new MathContext(32));
    }

    public static BigDecimal divide(double a, BigDecimal b) {
        return BigDecimal.valueOf(a).divide(b, new MathContext(32));
    }

    public static BigDecimal divide(BigDecimal a, double b) {
        return a.divide(BigDecimal.valueOf(b), new MathContext(32));
    }

    public static BigDecimal divide(double a, double b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), new MathContext(32));
    }

    public static BigDecimal divide(long a, double b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), new MathContext(32));
    }

    public static BigDecimal divide(long a, long b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), new MathContext(32));
    }

    public static BigDecimal divide(double a, long b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), new MathContext(32));
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.min(b);
    }

    public static BigDecimal floor(BigDecimal a) {
        return a.setScale(0, RoundingMode.FLOOR);
    }

    public static boolean isLowerOrEquals(BigDecimal a, double b) {
        return a.compareTo(BigDecimal.valueOf(b)) <= 0;
    }

    public static boolean isZero(BigDecimal a) {
        return a.compareTo(BigDecimal.ZERO) == 0;
    }
}
