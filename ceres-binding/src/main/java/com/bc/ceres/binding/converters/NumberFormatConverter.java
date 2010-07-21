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

import java.text.NumberFormat;

public class NumberFormatConverter implements com.bc.ceres.binding.Converter<Object> {

    private NumberFormat format;

    public NumberFormatConverter(NumberFormat format) {
        this.format = format;
    }

    @Override
    public Class<?> getValueType() {
        return Number.class;
    }

    @Override
    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return format.parseObject(text);
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String format(Object value) {
        return format.format(value);
    }
}
