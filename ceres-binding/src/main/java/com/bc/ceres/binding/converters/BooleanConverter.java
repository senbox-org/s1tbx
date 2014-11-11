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

public class BooleanConverter implements Converter<Boolean> {
    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Boolean parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        if (text.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (text.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (text.equalsIgnoreCase("0")) {
            return Boolean.FALSE;
        }
        try {
            return Double.parseDouble(text) != 0.0 ? Boolean.TRUE : Boolean.FALSE;
        } catch (NumberFormatException e) {
            return Boolean.FALSE;
        }
    }

    @Override
    public String format(Boolean value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
