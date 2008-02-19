package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;

import java.text.MessageFormat;

public class NotNullValidator implements Validator {
    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        if (value == null) {
            throw new ValidationException(MessageFormat.format("No value for ''{0}'' specified.", 
                                                               valueModel.getDescriptor().getDisplayName()));
        }
    }
}
