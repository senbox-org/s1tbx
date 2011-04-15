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

public class EnumConverterTest extends AbstractConverterTest {

    private EnumConverter<Tests> converter;

    private enum Tests {

        TEST1,
        TEST2 {
            @Override
            public String toString() {
                return "Test 2";

            }
        },
        TEST3
    }

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new EnumConverter<Tests>(Tests.class);
        }
        return converter;
    }


    @Override
    public void testConverter() throws ConversionException {
        testValueType(Tests.class);

        testParseSuccess(Tests.TEST1, "TEST1");
        testParseSuccess(Tests.TEST2, "TEST2");
        testParseSuccess(Tests.TEST3, "TEST3");
        testParseSuccess(null, "");

        testParseFailed("test1");
        testParseFailed("Test 2");

        testFormatSuccess("TEST1", Tests.TEST1);
        testFormatSuccess("TEST2", Tests.TEST2);
        testFormatSuccess("TEST3", Tests.TEST3);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }

    public void testConverterForwardAndBackward() throws ConversionException {
        final Converter converter = getConverter();
        assertEquals(Tests.TEST1, converter.parse(converter.format(Tests.TEST1)));
        assertEquals(Tests.TEST2, converter.parse(converter.format(Tests.TEST2)));
        assertEquals(Tests.TEST3, converter.parse(converter.format(Tests.TEST3)));
    }
}
