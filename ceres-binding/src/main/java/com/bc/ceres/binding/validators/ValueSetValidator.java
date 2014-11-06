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
        if (value != null || propertyDescriptor.isNotNull()) {
            if (!propertyDescriptor.getValueSet().contains(value)) {
                throw new ValidationException(MessageFormat.format("Value for ''{0}'' is invalid: ''{1}''",
                                                                   property.getDescriptor().getDisplayName(), value));
            }
        }
    }
}
