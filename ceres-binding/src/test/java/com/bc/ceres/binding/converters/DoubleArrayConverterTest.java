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

public class DoubleArrayConverterTest extends AbstractConverterTest {

    private ArrayConverter converter;


    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new ArrayConverter(double[].class, new DoubleConverter());
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(double[].class);
        testParseSuccess(new double[]{-2.3, -1.1, 0.09, 1.8, 2.1, 3.45, 4.3}, "-2.3,-1.1, 0.09,1.8  \n,2.1,3.45,4.3");
        testFormatSuccess("-2.3,-1.1,0.09,1.8,2.1,3.45,4.3", new double[]{-2.3, -1.1, 0.09, 1.8, 2.1, 3.45, 4.3});
        assertNullCorrectlyHandled();
    }
}