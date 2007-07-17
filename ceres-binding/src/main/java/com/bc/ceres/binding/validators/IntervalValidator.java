package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.Interval;

public class IntervalValidator implements Validator {
    private final Interval interval;

    public IntervalValidator(Interval interval) {
        this.interval = interval;
    }

    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        if (value instanceof Number) {
            if (!interval.contains(((Number) value).doubleValue())) {
                throw new ValidationException("Value for '" + valueModel.getDefinition().getDisplayName() + "' is out of range '" + interval + "'.");
            }
        }
    }
}
