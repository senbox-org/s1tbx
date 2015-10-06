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

package org.esa.snap.dataio.envisat;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.datamodel.ProductData;

public class FieldTest extends TestCase {

    Field[] _fields;

    public FieldTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(FieldTest.class);
    }

    @Override
    protected void setUp() {

        FieldInfo[] fieldInfos = new FieldInfo[]{
                new FieldInfo("_fiByte", ProductData.TYPE_INT8, 4, null, null),
                new FieldInfo("_fiUByte", ProductData.TYPE_UINT8, 4, null, null),
                new FieldInfo("_fiShort", ProductData.TYPE_INT16, 4, null, null),
                new FieldInfo("_fiUShort", ProductData.TYPE_UINT16, 4, null, null),
                new FieldInfo("_fiInt", ProductData.TYPE_INT32, 4, null, null),
                new FieldInfo("_fiUInt", ProductData.TYPE_UINT32, 4, null, null),
                new FieldInfo("_fiFloat", ProductData.TYPE_FLOAT32, 4, null, null),
                new FieldInfo("_fiDouble", ProductData.TYPE_FLOAT64, 4, null, null),
                new FieldInfo("_fiUTC", ProductData.TYPE_UTC, 4, null, null),
                new FieldInfo("_fiAscii", ProductData.TYPE_ASCII, 4, null, null),
        };

        Object[] fieldValues = new Object[]{
                new byte[]{1, 2, -3, -4},
                new byte[]{1, 2, (byte) (Byte.MAX_VALUE + 3), (byte) (Byte.MAX_VALUE + 4)},
                new short[]{1, 2, -3, -4},
                new short[]{1, 2, (short) (Short.MAX_VALUE + 3), (short) (Short.MAX_VALUE + 4)},
                new int[]{1, 2, -3, -4},
                new int[]{1, 2, (Integer.MAX_VALUE + 3), (Integer.MAX_VALUE + 4)},
                new float[]{1.0F, 2.0F, -3.0F, -4.0F},
                new double[]{1.0, 2.0, -3.0, -4.0},
                new int[]{1, 2, 3},
                "ABCD".getBytes()
        };

        _fields = new Field[fieldInfos.length];
        for (int i = 0; i < fieldInfos.length; i++) {
            _fields[i] = fieldInfos[i].createField();
            _fields[i].setValue(fieldValues[i]);
        }
    }

    @Override
    protected void tearDown() {
        _fields = null;
    }

    public void testCreate() {
        try {
            new Field(null);
            fail("IllegalArgumentException expected: field info must not be null");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetData() {
        assertTrue(_fields[0].getElems() instanceof byte[]);
        assertTrue(_fields[1].getElems() instanceof byte[]);
        assertTrue(_fields[2].getElems() instanceof short[]);
        assertTrue(_fields[3].getElems() instanceof short[]);
        assertTrue(_fields[4].getElems() instanceof int[]);
        assertTrue(_fields[5].getElems() instanceof int[]);
        assertTrue(_fields[6].getElems() instanceof float[]);
        assertTrue(_fields[7].getElems() instanceof double[]);
        assertTrue(_fields[8].getElems() instanceof int[]);
        assertTrue(_fields[9].getElems() instanceof byte[]);
    }

    public void testGetDataType() {
        assertEquals(ProductData.TYPE_INT8, _fields[0].getDataType());
        assertEquals(ProductData.TYPE_UINT8, _fields[1].getDataType());
        assertEquals(ProductData.TYPE_INT16, _fields[2].getDataType());
        assertEquals(ProductData.TYPE_UINT16, _fields[3].getDataType());
        assertEquals(ProductData.TYPE_INT32, _fields[4].getDataType());
        assertEquals(ProductData.TYPE_UINT32, _fields[5].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, _fields[6].getDataType());
        assertEquals(ProductData.TYPE_FLOAT64, _fields[7].getDataType());
        assertEquals(ProductData.TYPE_UTC, _fields[8].getDataType());
        assertEquals(ProductData.TYPE_ASCII, _fields[9].getDataType());
    }

    /*
    public void testGetValueAsText() {
        assertTrue(false);
    }
    public void testGetElemDouble() {
        assertTrue(false);
    }
    public void testGetElemFloat() {
        assertTrue(false);
    }
    public void testGetElemInt() {
        assertTrue(false);
    }
    public void testGetInfo() {
        assertTrue(false);
    }
    public void testGetName() {
        assertTrue(false);
    }
    public void testGetNumElems() {
        assertTrue(false);
    }
    public void testIsIntType() {
        assertTrue(false);
    }
    public void testReadFrom() {
        assertTrue(false);
    }
    public void testSetData() {
        assertTrue(false);
    }
    public void testToString() {
        assertTrue(false);
    }
 */
}
