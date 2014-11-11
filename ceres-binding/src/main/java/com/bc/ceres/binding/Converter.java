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

/**
 * A {@code Converter} provides a strategy to convert a value of a certain type from
 * plain text to a Java object and vice versa.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public interface Converter<T> {
    /**
     * Gets the value type.
     *
     * @return The value type.
     */
    Class<? extends T> getValueType();

    /**
     * Converts a value from its plain text representation to a Java object instance
     * of the type returned by {@link #getValueType()}.
     *
     * @param text The textual representation of the value.
     * @return The converted value.
     * @throws ConversionException If the conversion fails.
     */
    T parse(String text) throws ConversionException;

    /**
     * Converts a value of the type returned by {@link #getValueType()} to its
     * plain text representation.
     *
     * @param value The value to be converted to text.
     * @return The textual representation of the value.
     */
    String format(T value);
}
