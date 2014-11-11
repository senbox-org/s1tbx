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

public class ByteConverterTest extends AbstractConverterTest {

    private ByteConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ByteConverter();
        }
        return converter;
    }


    @Override
    public void testConverter() throws ConversionException {
        testValueType(Byte.class);

        testParseSuccess((byte) 124, "124");
        testParseSuccess((byte) -45, "-45");
        testParseSuccess((byte) 45, "+45");
        testParseSuccess(null, "");

        testFormatSuccess("124", (byte) 124);
        testFormatSuccess("-6", (byte) -6);
        testFormatSuccess("45", (byte) 45);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
