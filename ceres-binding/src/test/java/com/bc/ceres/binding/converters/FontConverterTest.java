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

public class FontConverterTest extends AbstractConverterTest {

    private FontConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new FontConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Font.class);

        testParseSuccess(new Font("times", Font.PLAIN, 10), "times-plain-10");
        testParseSuccess(new Font("times", Font.BOLD, 11), "times-bold-11");
        testParseSuccess(new Font("times", Font.ITALIC, 12), "times-italic-12");
        testParseSuccess(new Font("times", Font.BOLD | Font.ITALIC, 14), "times-bolditalic-14");
        testParseSuccess(null, "");

        testFormatSuccess("times-plain-10", new Font("times", Font.PLAIN, 10));
        testFormatSuccess("times-bold-11", new Font("times", Font.BOLD, 11));
        testFormatSuccess("times-italic-12", new Font("times", Font.ITALIC, 12));
        testFormatSuccess("times-bolditalic-14", new Font("times", Font.BOLD | Font.ITALIC, 14));
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
