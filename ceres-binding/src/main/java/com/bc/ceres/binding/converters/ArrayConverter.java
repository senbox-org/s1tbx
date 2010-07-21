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

import java.lang.reflect.Array;
import java.util.StringTokenizer;

public class ArrayConverter implements Converter<Object> {

    public static final String SEPARATOR = ",";
    public static final String SEPARATOR_ESC = "\\u002C"; // Unicode escape repr. of ','
    private Class<?> arrayType;
    private Converter componentConverter;

    public ArrayConverter(Class<?> arrayType, Converter componentConverter) {
        this.arrayType = arrayType;
        this.componentConverter = componentConverter;
    }

    @Override
    public Class<?> getValueType() {
        return arrayType;
    }

    @Override
    public Object parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(text, SEPARATOR);
        int length = st.countTokens();
        Object array = Array.newInstance(arrayType.getComponentType(), length);
        for (int i = 0; i < length; i++) {
            Object component = componentConverter.parse(st.nextToken().replace(SEPARATOR_ESC, SEPARATOR));
            Array.set(array, i, component);
        }
        return array;
    }

    @Override
    public String format(Object array) {
        if (array == null) {
            return "";
        }
        int length = Array.getLength(array);
        StringBuilder sb = new StringBuilder(length * 4);
        for (int i = 0; i < length; i++) {
            Object component = Array.get(array, i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append(componentConverter.format(component).replace(SEPARATOR, SEPARATOR_ESC));
        }
        return sb.toString();
    }
}