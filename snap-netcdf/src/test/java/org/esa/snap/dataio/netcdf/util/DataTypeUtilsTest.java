/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.util;

import org.junit.Test;
import ucar.ma2.DataType;

import static org.junit.Assert.*;

public class DataTypeUtilsTest {

    @Test
    public void testConvertToBYTE() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.BYTE);
        assertEquals((byte) 12, convertedNumber);
        assertEquals(DataType.BYTE, DataType.getType(convertedNumber.getClass()));
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.BYTE);
        assertEquals((byte) -123, convertedNumber);
        assertEquals(DataType.BYTE, DataType.getType(convertedNumber.getClass()));
    }

    @Test
    public void testConvertToSHORT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.SHORT);
        assertEquals(DataType.SHORT, DataType.getType(convertedNumber.getClass()));
        assertEquals((short) 12, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.SHORT);
        assertEquals(DataType.SHORT, DataType.getType(convertedNumber.getClass()));
        assertEquals((short) -123, convertedNumber);
    }

    @Test
    public void testConvertToINT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.INT);
        assertEquals(DataType.INT, DataType.getType(convertedNumber.getClass()));
        assertEquals(12, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.INT);
        assertEquals(DataType.INT, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123, convertedNumber);
    }

    @Test
    public void testConvertToLONG() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.LONG);
        assertEquals(DataType.LONG, DataType.getType(convertedNumber.getClass()));
        assertEquals(12L, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.LONG);
        assertEquals(DataType.LONG, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123L, convertedNumber);
    }

    @Test
    public void testConvertToFLOAT() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.FLOAT);
        assertEquals(DataType.FLOAT, DataType.getType(convertedNumber.getClass()));
        assertEquals(12.3f, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.FLOAT);
        assertEquals(DataType.FLOAT, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123f, convertedNumber);
    }

    @Test
    public void testConvertToDOUBLE() {
        Number convertedNumber = DataTypeUtils.convertTo(12.3, DataType.DOUBLE);
        assertEquals(DataType.DOUBLE, DataType.getType(convertedNumber.getClass()));
        assertEquals(12.3, convertedNumber);
        convertedNumber = DataTypeUtils.convertTo(-123, DataType.DOUBLE);
        assertEquals(DataType.DOUBLE, DataType.getType(convertedNumber.getClass()));
        assertEquals(-123.0, convertedNumber);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertToWithIllegalArgument() {
        DataTypeUtils.convertTo(12.3, DataType.STRING);
    }
}
