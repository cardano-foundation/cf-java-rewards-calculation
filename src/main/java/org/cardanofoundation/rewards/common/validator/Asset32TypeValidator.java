package org.cardanofoundation.rewards.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static org.cardanofoundation.rewards.constants.ValidationConstant.ASSET_MAX_BYTES;

public class Asset32TypeValidator implements ConstraintValidator<Asset32Type, byte[]> {

    /**
     * Checking if input bytes length equal to 58 or not . if equal return true  else false
     *
     * @param data                       byte array
     * @param constraintValidatorContext
     * @return boolean
     */
    @Override
    public boolean isValid(byte[] data, ConstraintValidatorContext constraintValidatorContext) {
        return data.length == ASSET_MAX_BYTES;
    }
}