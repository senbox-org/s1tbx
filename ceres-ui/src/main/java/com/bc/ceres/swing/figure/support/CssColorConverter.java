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

package com.bc.ceres.swing.figure.support;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;

import java.awt.Color;

public class CssColorConverter implements Converter<Color> {

    /**
     * Gets the value type.
     *
     * @return The value type.
     */
    @Override
    public Class<? extends Color> getValueType() {
        return Color.class;
    }

    /**
     * Converts a value from its plain text representation to a Java object instance
     * of the type returned by {@link #getValueType()}.
     *
     * @param text The textual representation of the value. It must be either in hexadecimal format (e.g. "#FFFFFF" or
     *             "FFFFFF") or given by a string of comma-separated numbers, such as "255,255,255".
     * @return The converted value.
     * @throws com.bc.ceres.binding.ConversionException
     *          If the conversion fails.
     */
    @Override
    public Color parse(String text) throws ConversionException {
        // todo - parse CSS stuff like "pink", "black", etc.
        try {
            final String[] rgbValues = text.split(",");
            final boolean isCommaSeparatedColorValue = rgbValues.length == 3;
            if (isCommaSeparatedColorValue) {
                return new Color(Integer.parseInt(rgbValues[0]), Integer.parseInt(rgbValues[1]), Integer.parseInt(rgbValues[2]));
            }
            return Color.decode("0x" + (text.startsWith("#") ? text.substring(1) : text));
        } catch (NumberFormatException e) {
            throw new ConversionException(e);
        }
    }

    /**
     * Converts a value of the type returned by {@link #getValueType()} to its
     * plain text representation.
     *
     * @param value The value to be converted to text.
     * @return The textual representation of the value.
     */
    @Override
    public String format(Color value) {
        StringBuilder sb = new StringBuilder("#");
        append2DigitHex(sb, value.getRed());
        append2DigitHex(sb, value.getGreen());
        append2DigitHex(sb, value.getBlue());
        return sb.toString();
    }

    private void append2DigitHex(StringBuilder sb, int red) {
        String s = Integer.toHexString(red);
        if (s.length() == 1) {
            sb.append("0");
            sb.append(s);
        } else if (s.length() == 2) {
            sb.append(s);
        } else {
            sb.append("00");
        }
    }
}
