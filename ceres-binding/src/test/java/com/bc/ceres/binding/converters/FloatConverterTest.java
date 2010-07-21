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

public class FloatConverterTest extends AbstractConverterTest {

    private FloatConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new FloatConverter();
        }
        return converter;
    }


    @Override
    public void testConverter() throws ConversionException {
        testValueType(Float.class);

        testParseSuccess(234f, "234");
        testParseSuccess(-45.789f, "-45.789");
        testParseSuccess(0.25f, "+0.25");
        testParseSuccess(null, "");
        testParseSuccess(Float.NaN, "NaN");

        testFormatSuccess("2353465.0", 2353465f);
        testFormatSuccess("-6.0", -6f);
        testFormatSuccess("0.0789", 0.0789f);
        testFormatSuccess("NaN", Float.NaN);

        assertNullCorrectlyHandled();
    }
}
