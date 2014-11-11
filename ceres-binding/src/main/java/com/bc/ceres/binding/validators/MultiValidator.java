/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
