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

        TEST1("Description 1") {
            @Override
            public String toString() {
                return "Test 1";
            }
        },
        TEST2("Description 2") {
            @Override
            public String toString() {
                return "Test 2";
            }
        },
        TEST3("Description 3") {
            @Override
            public String toString() {
                return "Test 3";
            }
        };

        private String description;

        private Tests(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
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

        testFormatSuccess("Test 1", Tests.TEST1);
        testFormatSuccess("Test 2", Tests.TEST2);
        testFormatSuccess("Test 3", Tests.TEST3);
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();
    }
}
