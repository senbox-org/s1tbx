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

import java.lang.reflect.Field;

/**
 * ValueAccessor for values stored in a {@link Field} of a plain Java object.
 */
public class ClassFieldAccessor implements PropertyAccessor {
    private Object object;
    private Field field;

    /**
     * Constructs a new {@code ClassFieldAccessor}.
     *
     * @param instance the plain Java object
     * @param field    the field which holds the value
     */
    public ClassFieldAccessor(Object instance, Field field) {
        this.object = instance;
        this.field = field;
    }

    /**
     * {@inheritDoc}
     */
    public Object getValue() {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (!accessible) {
                field.setAccessible(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(Object value) {
        boolean accessible = field.isAccessible();
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } finally {
            if (!accessible) {
                field.setAccessible(false);
            }
        }
    }
}
