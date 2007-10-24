package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;

import java.util.regex.Pattern;

public class PatternValidator implements Validator {
    private final Pattern pattern;

    public PatternValidator(Pattern pattern) {
        this.pattern = pattern;
    }

    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        if (!pattern.matcher(value.toString()).matches()) {
            throw new ValidationException("Value for '" + valueModel.getDescriptor().getDisplayName() + "' does not match '" + pattern + "'.");
        }
    }
}
