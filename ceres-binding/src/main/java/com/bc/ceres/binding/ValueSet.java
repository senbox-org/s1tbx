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

import com.bc.ceres.binding.converters.ArrayConverter;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A set of values allowed to be assigned to certain types of values. This set
 * can be a property of a {@link PropertyDescriptor}.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueSet {

    private List<Object> objects;

    public ValueSet(Object[] items) {
        this.objects = Arrays.asList(items);
    }

    public Object[] getItems() {
        return objects.toArray();
    }

    public boolean contains(Object value) {
        return objects.contains(value);
    }

    /**
     * Converts a comma-separated text string into a value set.
     *
     * @param text     The textual representation of the value set. Commas in values must be escaped using UNICODE character encoding.
     * @param itemType The type of the value set's items.
     * @return The value set.
     * @throws IllegalArgumentException If the text has an invalid format.
     */
    public static ValueSet parseValueSet(String text, Class<?> itemType) throws IllegalArgumentException {
        final Converter converter = ConverterRegistry.getInstance().getConverter(itemType);
        if (converter == null) {
            throw new IllegalArgumentException("itemType");
        }
        StringTokenizer st = new StringTokenizer(text, ArrayConverter.SEPARATOR);
        Object[] values = new Object[st.countTokens()];
        for (int i = 0; i < values.length; i++) {
            try {
                values[i] = converter.parse(st.nextToken().trim().replace(ArrayConverter.SEPARATOR_ESC, ArrayConverter.SEPARATOR));
            } catch (ConversionException e) {
                throw new IllegalArgumentException("text", e);
            }
        }
        return new ValueSet(values);
    }

    /**
     * Converts a string array into a value set.
     *
     * @param valueStrings  The textual representation of the items of the value set.
     * @param itemConverter The converter to be used for the item conversion.
     * @return The value set.
     * @throws ConversionException If the conversion of one of the items fails.
     */
    public static ValueSet parseValueSet(String[] valueStrings, Converter itemConverter) throws ConversionException {
        Object[] objects = new Object[valueStrings.length];
        for (int i = 0; i < valueStrings.length; i++) {
            objects[i] = itemConverter.parse(valueStrings[i]);
        }
        return new ValueSet(objects);
    }
}
