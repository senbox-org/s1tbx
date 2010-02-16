package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.Property;

import java.util.ArrayList;
import java.util.List;

public class MultiValidator implements Validator {
    private final List<Validator> validators;

    public MultiValidator() {
        this.validators = new ArrayList<Validator>(3);
    }

    public MultiValidator(List<Validator> validators) {
        this.validators = validators;
    }

    public Validator[] getValidators() {
        return validators.toArray(new Validator[validators.size()]);
    }

    public void addValidator(Validator validator) {
        validators.add(validator);
    }

    public void removeValidator(Validator validator) {
        validators.remove(validator);
    }

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        for (Validator validator : validators) {
            validator.validateValue(property, value);
        }
    }
}
