package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;

public class ValueSetValidator implements Validator {
    private final ValueDescriptor valueDescriptor;

    public ValueSetValidator(ValueDescriptor valueDescriptor) {
        this.valueDescriptor = valueDescriptor;
    }

    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        if (!valueDescriptor.getValueSet().contains(value)) {
            throw new ValidationException("Value for '" + valueModel.getDescriptor().getDisplayName() + "' is invalid.");
        }
    }
}
