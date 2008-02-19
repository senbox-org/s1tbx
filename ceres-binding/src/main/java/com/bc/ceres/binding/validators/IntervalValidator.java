package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueRange;

import java.text.MessageFormat;

public class IntervalValidator implements Validator {
    private final ValueRange valueRange;

    public IntervalValidator(ValueRange valueRange) {
        this.valueRange = valueRange;
    }

    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        if (value instanceof Number) {
            if (!valueRange.contains(((Number) value).doubleValue())) {
                throw new ValidationException(MessageFormat.format("Value for ''{0}'' is out of range ''{1}''.", 
                                                                   valueModel.getDescriptor().getDisplayName(),
                                                                   valueRange));
            }
        }
    }
}
