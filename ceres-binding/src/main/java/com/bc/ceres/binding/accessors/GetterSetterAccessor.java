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

package com.bc.ceres.binding.accessors;

import com.bc.ceres.binding.PropertyAccessor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * ValueAccessor for values stored in a {@link java.lang.reflect.Field} of a plain Java object.
 *
 * @author Norman
 * @since Ceres 0.14
 */
public class GetterSetterAccessor implements PropertyAccessor {
    private final Object object;
    private final Method getter;
    private final Method setter;

    /**
     * Constructs a new {@code ClassFieldAccessor}.
     *
     * @param instance the plain Java object
     * @param getter    the getter method
     * @param setter    the setter method
     */
    public GetterSetterAccessor(Object instance, Method getter, Method setter) {
        this.object = instance;
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValue() {
        try {
            return getter.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Object value) {
        try {
            setter.invoke(object, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
