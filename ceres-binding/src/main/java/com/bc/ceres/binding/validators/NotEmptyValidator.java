package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;

public class NotEmptyValidator implements Validator {
    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new ValidationException("No value for '" + valueModel.getDescriptor().getDisplayName() + "' specified.");
        }
    }
}
