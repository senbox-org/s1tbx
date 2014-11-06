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

package com.bc.ceres.binding;

import java.lang.reflect.Field;

/**
 * A factory for property descriptors derived from Java class {@link Field}s.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface PropertyDescriptorFactory {
    /**
     * Creates a new property descriptor for the given field.
     *
     * @param field The field.
     * @return The property descriptor or {@code null}, if it cannot be derived from the given field.
     */
    PropertyDescriptor createValueDescriptor(Field field);
}
