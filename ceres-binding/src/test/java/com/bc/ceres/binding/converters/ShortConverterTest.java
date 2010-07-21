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

public class ShortConverterTest extends AbstractConverterTest {

    private ShortConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ShortConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(Short.class);

        testParseSuccess((short) 234, "234");
        testParseSuccess((short) -45, "-45");
        testParseSuccess((short) 45, "+45");
        testParseSuccess(null, "");

        testFormatSuccess("235", (short) 235);
        testFormatSuccess("-6", (short) -6);
        testFormatSuccess("45", (short) 45);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
