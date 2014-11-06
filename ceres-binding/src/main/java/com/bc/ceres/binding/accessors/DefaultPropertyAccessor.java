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

/**
 * A value accessor for values stored as plain Java object.
 */
public class DefaultPropertyAccessor implements PropertyAccessor {
    private Object value;

    /**
     * Constructs a new {@code DefaultValueAccessor} with a {@code null} default value.
     */
    public DefaultPropertyAccessor() {
    }

    /**
     * Constructs a new {@code DefaultValueAccessor} with a {@code null} default value.
     * @param value The initial value.
     */
    public DefaultPropertyAccessor(Object value) {
        this.value = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Object value) {
        this.value = value;
    }
}
