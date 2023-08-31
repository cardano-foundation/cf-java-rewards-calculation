package org.cardanofoundation.rewards.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static org.cardanofoundation.rewards.constants.ValidationConstant.HASH_28;

public class Hash28TypeValidator implements ConstraintValidator<Hash28Type, String> {


    /**
     * Checking if input string length equal to 56 or not . if equal return true  else false
     *
     * @param string hash string
     * @param constraintValidatorContext constraintValidatorContext
     * @return boolean
     */
    @Override
    public boolean isValid(String string, ConstraintValidatorContext constraintValidatorContext) {
        return string.length() == HASH_28;
    }
}
