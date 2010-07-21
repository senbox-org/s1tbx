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

import java.awt.geom.AffineTransform;

public class AffineTransformConverterTest extends AbstractConverterTest {

    private AffineTransformConverter converter;

    @Override
    public Converter getConverter() {
        if (converter == null) {
            converter = new AffineTransformConverter();
        }
        return converter;
    }

    @Override
    public void testConverter() throws ConversionException {
        testValueType(AffineTransform.class);

        testParseSuccess(new AffineTransform(1.0, 2.0, 3.0, 4.0, 5.0, 6.0), "1.0, 2.0,3.0,4.0,5.0,6.0");
        testParseSuccess(new AffineTransform(1.0, 2.0, 3.0, 4.0, 0.0, 0.0), "1.0,\t2.0,3.0,4.0");
        testParseSuccess(null, "");

        testFormatSuccess("1.0,2.0,3.0,4.0,5.0,6.0", new AffineTransform(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
        testFormatSuccess("", null);

        assertNullCorrectlyHandled();

        try {
            converter.parse("1.0,2.0");
            fail();
        } catch (ConversionException expected) {
            // ignore
        }
    }
}
