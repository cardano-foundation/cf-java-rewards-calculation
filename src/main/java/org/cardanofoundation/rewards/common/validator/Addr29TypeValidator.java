package org.cardanofoundation.rewards.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static org.cardanofoundation.rewards.constants.ValidationConstant.ADDRESS_MAX_BYTES;

public class Addr29TypeValidator implements ConstraintValidator<Addr29Type, String> {

    @Override
    public boolean isValid(String bytes, ConstraintValidatorContext constraintValidatorContext) {
        return bytes.length() == ADDRESS_MAX_BYTES;
    }
}