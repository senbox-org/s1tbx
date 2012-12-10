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

package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

public abstract class NumberConverter<T extends Number> implements Converter<T> {

    @Override
    public abstract Class<? extends T> getValueType();

    @Override
    public T parse(String value) throws ConversionException {
        if (value.isEmpty()) {
            return null;
        }
        try {
            return parseNumber(trimNumberString(value));
        } catch (NumberFormatException e) {
            throw new ConversionException("'" + value + "' cannot be converted to a " + getValueType().getSimpleName().toLowerCase() + ".");
        }
    }

    protected abstract T parseNumber(String value) throws NumberFormatException;

    @Override
    public String format(T value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    private static String trimNumberString(String s) {
        s = s.trim();
        if (s.startsWith("+")) {
            s = s.substring(1);
        }
        return s;
    }
}
