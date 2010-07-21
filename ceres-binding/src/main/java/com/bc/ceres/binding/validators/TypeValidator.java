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

import java.text.MessageFormat;

public class TypeValidator implements Validator {

    @Override
    public void validateValue(Property property, Object value) throws ValidationException {
        final Class<?> type = property.getDescriptor().getType();
        if (!isAssignableFrom(type, value)) {
            throw new ValidationException(MessageFormat.format("Value for ''{0}'' must be of type ''{1}''.",
                                                               property.getDescriptor().getDisplayName(),
                                                               type.getSimpleName()));
        }
    }

    static boolean isAssignableFrom(Class<?> type, Object value) {
        if (value == null) {
            return !type.isPrimitive();
        }
        final Class<?> valueType = value.getClass();
        return type.isAssignableFrom(valueType)
               || type.isPrimitive()
                  && (type.equals(Boolean.TYPE) && valueType.equals(Boolean.class)
                      || type.equals(Character.TYPE) && valueType.equals(Character.class)
                      || type.equals(Byte.TYPE) && valueType.equals(Byte.class)
                      || type.equals(Short.TYPE) && valueType.equals(Short.class)
                      || type.equals(Integer.TYPE) && valueType.equals(Integer.class)
                      || type.equals(Long.TYPE) && valueType.equals(Long.class)
                      || type.equals(Float.TYPE) && valueType.equals(Float.class)
                      || type.equals(Double.TYPE) && valueType.equals(Double.class));
    }
}
