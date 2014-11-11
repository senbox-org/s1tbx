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

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;

/**
 * Class for converting enumeration types.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class EnumConverter<T extends Enum<T>> implements Converter<T> {
    private Class<T> type;

    public EnumConverter(Class<T> type) {
        this.type = type;
    }

    @Override
    public Class<T> getValueType() {
        return type;
    }

    @Override
    public T parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Enum.valueOf(type, text);
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public String format(T value) {
        if (value == null) {
            return "";
        }
        return value.name();
    }

}
