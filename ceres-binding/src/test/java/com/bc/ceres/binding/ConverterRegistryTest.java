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

package com.bc.ceres.binding;

import junit.framework.TestCase;

import java.io.File;
import java.util.Date;
import java.util.regex.Pattern;

public class ConverterRegistryTest extends TestCase {

    public void testPrimitiveTypes()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();
        assertNotNull(r.getConverter(Boolean.TYPE));
        assertNotNull(r.getConverter(Character.TYPE));
        assertNotNull(r.getConverter(Byte.TYPE));
        assertNotNull(r.getConverter(Short.TYPE));
        assertNotNull(r.getConverter(Integer.TYPE));
        assertNotNull(r.getConverter(Long.TYPE));
        assertNotNull(r.getConverter(Float.TYPE));
        assertNotNull(r.getConverter(Double.TYPE));

        assertNotNull(r.getConverter(boolean[].class));
        assertNotNull(r.getConverter(char[].class));
        assertNotNull(r.getConverter(byte[].class));
        assertNotNull(r.getConverter(short[].class));
        assertNotNull(r.getConverter(int[].class));
        assertNotNull(r.getConverter(long[].class));
        assertNotNull(r.getConverter(float[].class));
        assertNotNull(r.getConverter(double[].class));
    }

    public void testPrimitiveTypeWrappers()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();
        assertNotNull(r.getConverter(Boolean.class));
        assertNotNull(r.getConverter(Character.class));
        assertNotNull(r.getConverter(Byte.class));
        assertNotNull(r.getConverter(Short.class));
        assertNotNull(r.getConverter(Integer.class));
        assertNotNull(r.getConverter(Long.class));
        assertNotNull(r.getConverter(Float.class));
        assertNotNull(r.getConverter(Double.class));

        assertNotNull(r.getConverter(Boolean[].class));
        assertNotNull(r.getConverter(Character[].class));
        assertNotNull(r.getConverter(Byte[].class));
        assertNotNull(r.getConverter(Short[].class));
        assertNotNull(r.getConverter(Integer[].class));
        assertNotNull(r.getConverter(Long[].class));
        assertNotNull(r.getConverter(Float[].class));
        assertNotNull(r.getConverter(Double[].class));
    }

    public void testObjects()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();
        assertNotNull(r.getConverter(String.class));
        assertNotNull(r.getConverter(File.class));
        assertNotNull(r.getConverter(ValueRange.class));
        assertNotNull(r.getConverter(Date.class));
        assertNotNull(r.getConverter(Pattern.class));
        // todo - assertNotNull(r.getConverter(Color.class));

        assertNotNull(r.getConverter(String[].class));
        assertNotNull(r.getConverter(File[].class));
        assertNotNull(r.getConverter(ValueRange[].class));
        assertNotNull(r.getConverter(Date[].class));
        assertNotNull(r.getConverter(Pattern[].class));

        assertNotNull(r.getConverter(U.class));
    }


    public void testDerivedObjects()  {
        final ConverterRegistry r = ConverterRegistry.getInstance();

        assertSame(r.getConverter(File.class), r.getConverter(MyFile.class));
        assertSame(r.getConverter(Date.class), r.getConverter(java.sql.Date.class));
    }

    enum U {
        A,B,C
    }

    private static class MyFile extends File {
        public MyFile() {
            super("");
        }
    }
}
