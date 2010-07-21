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

import java.lang.reflect.Array;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.Property;

public class ArrayValidator implements Validator {
    
    private final Validator validator;

    public ArrayValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        final int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            Object singleValue = Array.get(value, i);
            validator.validateValue(property, singleValue);
        }
    }
}
