package org.cardanofoundation.rewards.common.validator;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static org.cardanofoundation.rewards.constants.ValidationConstant.HASH_32;

public class Hash32TypeValidator implements ConstraintValidator<Hash32Type, String> {

    /**
     * Checking if input string length equal to 64 or not . if equal return true  else false
     *
     * @param string                     hash string
     * @param constraintValidatorContext
     * @return boolean
     */
    @Override
    public boolean isValid(String string, ConstraintValidatorContext constraintValidatorContext) {
        return string.length() == HASH_32;
    }
}
