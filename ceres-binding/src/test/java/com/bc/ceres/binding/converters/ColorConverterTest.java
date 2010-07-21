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

public class ColorConverterTest extends AbstractConverterTest {

    private ColorConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ColorConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Color.class);

        testParseSuccess(new Color(17, 11, 67), "17,11,67");
        testParseSuccess(new Color(17, 11, 67, 127), "17, \t11, 67, 127");
        testParseSuccess(null, "");

        testFormatSuccess("17,11,67", new Color(17, 11, 67));
        testFormatSuccess("17,11,67,127", new Color(17, 11, 67, 127));
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();

        try {
            converter.parse("17,11");
            fail();
        } catch (ConversionException expected) {
            // ignore
        }

        try {
            converter.parse("17,11,67,1024");
            fail();
        } catch (ConversionException expected) {
            // ignore
        }
    }
}
