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

import java.awt.Font;
import java.text.MessageFormat;

public class FontConverter implements Converter<Font> {

    public final static FontConverter INSTANCE = new FontConverter();

    @Override
    public Class<Font> getValueType() {
        return Font.class;
    }

    @Override
    public Font parse(String text) throws ConversionException {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Font.decode(text);
        } catch (Exception e) {
            throw new ConversionException("Illegal font format: " + text);
        }
    }

    @Override
    public String format(Font font) {
        if (font == null) {
            return "";
        }
        return String.format("%s-%s-%d", font.getName(), getStyleName(font), font.getSize());
    }

    private static String getStyleName(Font font) {
        final StringBuilder sb = new StringBuilder();

        if (font.isPlain()) {
            sb.append("plain");
        } else {
            if (font.isBold()) {
                sb.append("bold");
            }
            if (font.isItalic()) {
                sb.append("italic");
            }
        }

        return sb.toString();
    }
}
