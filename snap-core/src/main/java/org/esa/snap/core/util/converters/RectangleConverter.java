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

package org.esa.snap.core.util.converters;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import org.esa.snap.core.util.StringUtils;

import java.awt.Rectangle;

/**
 * A converter for Rectangle
 *
 * @author Luis Veci
 * @author Marco Peters
 */
public class RectangleConverter implements Converter<Rectangle> {


    private static final String EXCEPTION_FORMAT_PATTERN = "Invalid Rectangle '%s' should be in form of x,y,width,height";

    @Override
    public Class<Rectangle> getValueType() {
        return Rectangle.class;
    }

    @Override
    public Rectangle parse(String text) throws ConversionException {
        if (text != null) {
            text = text.trim();
        }
        if (text == null || text.isEmpty() || !text.contains(",")) {
            throw new ConversionException(String.format(EXCEPTION_FORMAT_PATTERN, text));
        }
        final String[] s = StringUtils.csvToArray(text);
        if (s.length != 4) {
            throw new ConversionException(String.format(EXCEPTION_FORMAT_PATTERN, text));
        }
        return new Rectangle(Integer.parseInt(s[0].trim()), Integer.parseInt(s[1].trim()),
                             Integer.parseInt(s[2].trim()), Integer.parseInt(s[3].trim()));
    }

    @Override
    public String format(final Rectangle r) {
        if (r == null) {
            return "0,0,0,0";
        }
        return "" + r.x + ',' + r.y + ',' + r.width + ',' + r.height;
    }
}
