package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValueRange;

import java.text.MessageFormat;

public class IntervalValidator implements Validator {
    private final ValueRange valueRange;

    public IntervalValidator(ValueRange valueRange) {
        this.valueRange = valueRange;
    }

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        if (value instanceof Number) {

            if (!valueRange.contains(((Number) value).doubleValue())) {
                final String message = MessageFormat.format("Value for ''{0}'' is out of range {1}.",
                                                            property.getDescriptor().getDisplayName(),
                                                            valueRange.toString());
                throw new ValidationException(message);
            }
        }
    }
}
