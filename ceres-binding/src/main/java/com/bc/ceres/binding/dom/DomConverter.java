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

package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;

/**
 * A {@code DomConverter} provides a strategy to convert a value of a certain type from
 * an (XML) DOM to a Java value and vice versa. Its common use is to convert complex value types
 * from and to XML.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @since 0.6
 */
public interface DomConverter {

    /**
     * Gets the value type, which may be abstract.
     *
     * @return The value type.
     */
    Class<?> getValueType();

    /**
     * Converts a DOM to a (complex) value of the type returned by {@link #getValueType()}.
     *
     * @param parentElement The parent DOM element.
     * @param value         The value which receives the DOM representation of the value or {@code null}.
     *                      If {@code value} is {@code null}, the method is responsible for the creation of a new instance
     *                      and its configuration using the DOM.
     * @return The converted value, never {@code null}.
     * @throws com.bc.ceres.binding.ConversionException
     *          If the conversion fails.
     * @throws com.bc.ceres.binding.ValidationException
     *          If the converted value is invalid.
     */
    Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException;

    /**
     * Converts a (complex) value of the type returned by {@link #getValueType()} to its
     * DOM representation which may be further converted to XML. The given {@code value} can be
     * safely cast to the type returned by {@link #getValueType()}.
     *
     * @param value         The value to be converted to the DOM. Never {@code null}.
     * @param parentElement The parent DOM element, which receives the DOM representation of the value.
     * @throws com.bc.ceres.binding.ConversionException
     *          If the conversion fails (e.g. not implemented).
     */
    void convertValueToDom(Object value, DomElement parentElement) throws ConversionException;
}
