package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.Property;

import java.text.MessageFormat;

public class ValueSetValidator implements Validator {
    private final PropertyDescriptor propertyDescriptor;

    public ValueSetValidator(PropertyDescriptor propertyDescriptor) {
        this.propertyDescriptor = propertyDescriptor;
    }

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        if (!propertyDescriptor.getValueSet().contains(value)) {
            throw new ValidationException(MessageFormat.format("Value for ''{0}'' is invalid.",
                                                               property.getDescriptor().getDisplayName()));
        }
    }
}
