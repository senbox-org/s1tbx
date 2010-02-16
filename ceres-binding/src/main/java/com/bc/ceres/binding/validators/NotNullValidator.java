package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.Property;

import java.text.MessageFormat;

public class NotNullValidator implements Validator {
    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        if (value == null) {
            throw new ValidationException(MessageFormat.format("Value for ''{0}'' must be given.", 
                                                               property.getDescriptor().getDisplayName()));
        }
    }
}
