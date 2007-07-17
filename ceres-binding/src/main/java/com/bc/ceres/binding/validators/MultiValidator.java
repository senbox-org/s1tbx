package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;

import java.util.ArrayList;
import java.util.List;

public class MultiValidator implements Validator {
    private List<Validator> validators;

    public MultiValidator() {
        this.validators = new ArrayList<Validator>(3);
    }

    public MultiValidator(List<Validator> validators) {
        this.validators = validators;
    }

    public void addValidator(Validator validator) {
        validators.add(validator);
    }

    public void removeValidator(Validator validator) {
        validators.remove(validator);
    }

    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        for (Validator validator : validators) {
            validator.validateValue(valueModel, value);
        }
    }
}
