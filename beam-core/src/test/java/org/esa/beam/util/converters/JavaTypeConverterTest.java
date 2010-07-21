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

package org.esa.beam.util.converters;

import junit.framework.TestCase;
import com.bc.ceres.binding.ConversionException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

import java.util.Date;

import org.esa.beam.util.converters.JavaTypeConverter;
import org.esa.beam.util.converters.JtsGeometryConverter;


public class JavaTypeConverterTest extends TestCase {
    static {
        JtsGeometryConverter.registerConverter();
    }

    public void testSuccess() throws ConversionException {
        JavaTypeConverter typeConverter = new JavaTypeConverter();

        assertEquals(String.class, typeConverter.parse("String"));
        assertEquals(Integer.class, typeConverter.parse("Integer"));
        assertEquals(Geometry.class, typeConverter.parse("Geometry"));
        assertEquals(Polygon.class, typeConverter.parse("Polygon"));
        assertEquals(Date.class, typeConverter.parse("Date"));
        assertEquals(JavaTypeConverter.class, typeConverter.parse("org.esa.beam.util.converters.JavaTypeConverter"));

        assertEquals("String", typeConverter.format(String.class));
        assertEquals("Integer", typeConverter.format(Integer.class));
        assertEquals("Geometry", typeConverter.format(Geometry.class));
        assertEquals("Polygon", typeConverter.format(Polygon.class));
        assertEquals("Date", typeConverter.format(Date.class));
        assertEquals("org.esa.beam.util.converters.JavaTypeConverter", typeConverter.format(JavaTypeConverter.class));
    }

    public void testFailure()  {
        JavaTypeConverter typeConverter = new JavaTypeConverter();

        try {
            typeConverter.parse("");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }

        try {
            typeConverter.parse("string");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }

        try {
            typeConverter.parse("java.util.String");
            fail("ConversionException expected.");
        } catch (ConversionException e) {
            // ok
        }
    }
}
