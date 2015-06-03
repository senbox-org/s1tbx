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

import java.awt.Color;
import java.text.MessageFormat;

public class ColorConverter implements Converter<Color> {

    public final static ColorConverter INSTANCE = new ColorConverter();

    @Override
    public Class<Color> getValueType() {
        return Color.class;
    }

    @Override
    public Color parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }

        final int[] array;
        try {
            array = (int[]) new ArrayConverter(int[].class, new IntegerConverter()).parse(text);
        } catch (ConversionException e) {
            throw new ConversionException(
                    MessageFormat.format("Cannot parse ''{0}'' into a color: {1}", text, e.getMessage()), e);
        }

        if (array.length < 3) {
            throw new ConversionException(
                    MessageFormat.format("Cannot parse ''{0}'' into a color.", text));
        }

        final int r = array[0];
        final int g = array[1];
        final int b = array[2];
        final int a;

        if (array.length == 4) {
            a = array[3];
        } else {
            a = 255;
        }

        try {
            return new Color(r, g, b, a);
        } catch (Exception e) {
            throw new ConversionException(
                    MessageFormat.format("Cannot parse ''{0}'' into a color: {1}", text, e.getMessage()), e);
        }
    }

    @Override
    public String format(Color color) {
        if (color == null) {
            return "";
        }
        
        final int r = color.getRed();
        final int g = color.getGreen();
        final int b = color.getBlue();
        final int a = color.getAlpha();


        if (a == 255) {
            return String.format("%d,%d,%d", r, g, b);
        } else {
            return String.format("%d,%d,%d,%d", r, g, b, a);
        }
    }
}
