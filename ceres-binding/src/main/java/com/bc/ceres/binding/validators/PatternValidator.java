package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.Property;

import java.util.regex.Pattern;
import java.text.MessageFormat;

public class PatternValidator implements Validator {
    private final Pattern pattern;

    public PatternValidator(Pattern pattern) {
        this.pattern = pattern;
    }

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        if (!pattern.matcher(value.toString()).matches()) {
            throw new ValidationException(MessageFormat.format("Value for ''{0}'' must match pattern ''{1}''.", 
                                                               property.getDescriptor().getDisplayName(),
                                                               pattern));
        }
    }
}
