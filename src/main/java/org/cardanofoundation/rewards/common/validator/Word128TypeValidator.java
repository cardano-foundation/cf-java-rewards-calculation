package org.cardanofoundation.rewards.common.validator;

import java.math.BigInteger;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.cardanofoundation.rewards.constants.ValidationConstant;

public class Word128TypeValidator implements ConstraintValidator<Word128Type, BigInteger> {

    private static final BigInteger MAX_128_BYTES = ValidationConstant.getMaxWord128();

    /**
     * Checking if input lovelace is out of range 2^128 or not
     * if not return true else false
     * @param number number
     * @param constraintValidatorContext constraintValidatorContext
     * @return boolean
     */
    @Override
    public boolean isValid(BigInteger number, ConstraintValidatorContext constraintValidatorContext) {
        return number.compareTo(BigInteger.ZERO) >= BigInteger.ZERO.intValue()
                || number.compareTo(MAX_128_BYTES)
                < BigInteger.ZERO.intValue();
    }
}