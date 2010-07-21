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

public class IntArrayConverterTest extends AbstractConverterTest {

    private ArrayConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ArrayConverter(int[].class, new IntegerConverter());
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(int[].class);
        testParseSuccess(new int[]{-2, -1, 0, 1, 2, 3, 4}, "-2,  -1,0  ,1,\t2,3,4");
        testFormatSuccess("-2,-1,0,1,2,3,4", new int[]{-2, -1, 0, 1, 2, 3, 4});
        assertNullCorrectlyHandled();
    }
}