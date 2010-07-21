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

public class DoubleConverterTest extends AbstractConverterTest {

    private DoubleConverter converter;


    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new DoubleConverter();
        }
        return converter;
    }


    @Override
    public void testConverter() throws ConversionException {
        testValueType(Double.class);

        testParseSuccess(234.0, "234");
        testParseSuccess(-45.789, "-45.789");
        testParseSuccess(0.25, "+0.25");
        testParseSuccess(null, "");
        testParseSuccess(Double.NaN, "NaN");

        testFormatSuccess("2353465.0", 2353465.0);
        testFormatSuccess("-6.0", -6.0);
        testFormatSuccess("0.0789", 0.0789);
        testFormatSuccess("", null);
        testFormatSuccess("NaN", Double.NaN);

        assertNullCorrectlyHandled();
    }
}
