package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueSet;

public class ValueSetValidator implements Validator {
    private final ValueSet valueSet;

    public ValueSetValidator(ValueSet valueSet) {
        this.valueSet = valueSet;
    }

    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        if (!valueSet.contains(value)) {
            throw new ValidationException("Value for '" + valueModel.getDescriptor().getDisplayName() + "' is invalid.");
        }
    }
}
