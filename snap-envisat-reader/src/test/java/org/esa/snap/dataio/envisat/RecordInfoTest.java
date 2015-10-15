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

public class RecordInfoTest extends TestCase {

    public RecordInfoTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(RecordInfoTest.class);
    }

    @Override
    protected void setUp() {
    }

    @Override
    protected void tearDown() {
    }

    public void testRecordInfo() {

        try {
            new RecordInfo("TEST");
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not occur");
        }

        try {
            new RecordInfo(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }

        try {
            new RecordInfo("");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testAdd() {
        RecordInfo recordInfo = new RecordInfo("test");
        try {
            recordInfo.add(null, ProductData.TYPE_UINT8, 4, null, null);
            fail("IllegalArgumentException expected: name must not be null");
        } catch (IllegalArgumentException e) {
        }

        try {
            recordInfo.add(null, 12349685, 4, null, null);
            fail("IllegalArgumentException expected: illegal data type");
        } catch (IllegalArgumentException e) {
        }

        try {
            recordInfo.add(null, ProductData.TYPE_UINT8, -4, null, null);
            fail("IllegalArgumentException expected: illegal num elements");
        } catch (IllegalArgumentException e) {
        }

        try {
            recordInfo.add((FieldInfo) null);
            fail("IllegalArgumentException expected: field must not be null");
        } catch (IllegalArgumentException e) {
        }

        try {
            recordInfo.add((RecordInfo) null);
            fail("IllegalArgumentException expected: field must not be null");
        } catch (IllegalArgumentException e) {
        }

        try {
            recordInfo.add("alpha", ProductData.TYPE_UINT8, 4, null, null);
        } catch (IllegalArgumentException e) {
            fail("IllegalArgumentException should not occur");
        }

        try {
            recordInfo.add("ALPHA", ProductData.TYPE_UINT8, 4, null, null);
            fail("IllegalArgumentException expected: name already exists");
        } catch (IllegalArgumentException e) {
        }
    }


    public void testGetFieldInfo() {
        RecordInfo recordInfo = new RecordInfo("test");
        recordInfo.add("ABC", ProductData.TYPE_INT8, 4, null, null);
        assertNotNull(recordInfo.getFieldInfo("ABC"));
        assertEquals("ABC", recordInfo.getFieldInfo("abc").getName());
        assertNotNull(recordInfo.getFieldInfo("abc"));
        assertNotNull(recordInfo.getFieldInfo("AbC"));
        assertNull(recordInfo.getFieldInfo("CBA"));
        try {
            recordInfo.getFieldInfo(null);
            fail("IllegalArgumentException expected: name must not be null");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetFieldInfoAt() {
        RecordInfo recordInfo = new RecordInfo("test");
        recordInfo.add("ABC", ProductData.TYPE_INT8, 4, null, null);
        assertNotNull(recordInfo.getFieldInfoAt(0));
        assertEquals("ABC", recordInfo.getFieldInfoAt(0).getName());
        try {
            recordInfo.getFieldInfoAt(1);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    public void testGetFieldInfoIndex() {
        RecordInfo recordInfo = new RecordInfo("test");
        recordInfo.add("ABC", ProductData.TYPE_INT8, 4, null, null);
        assertEquals(0, recordInfo.getFieldInfoIndex("ABC"));
        assertEquals(0, recordInfo.getFieldInfoIndex("abc"));
        assertEquals(0, recordInfo.getFieldInfoIndex("AbC"));
        assertEquals(-1, recordInfo.getFieldInfoIndex("CBA"));
        try {
            recordInfo.getFieldInfoIndex(null);
            fail("IllegalArgumentException expected: name must not be null");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testGetNumFieldInfos() {
        RecordInfo recordInfo = new RecordInfo("test");
        assertEquals(0, recordInfo.getNumFieldInfos());
        recordInfo.add("f1", ProductData.TYPE_INT8, 4, null, null);
        assertEquals(1, recordInfo.getNumFieldInfos());
        recordInfo.add("f2", ProductData.TYPE_INT8, 4, null, null);
        assertEquals(2, recordInfo.getNumFieldInfos());
        recordInfo.add("f3", ProductData.TYPE_INT8, 4, null, null);
        assertEquals(3, recordInfo.getNumFieldInfos());
    }

    public void testGetSizeInBytes() {
        RecordInfo recordInfo = new RecordInfo("test");
        int sib = 0;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f1", ProductData.TYPE_INT8, 4, null, null);
        sib += 1 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f2", ProductData.TYPE_UINT8, 4, null, null);
        sib += 1 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f3", ProductData.TYPE_INT16, 4, null, null);
        sib += 2 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f4", ProductData.TYPE_UINT16, 4, null, null);
        sib += 2 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f5", ProductData.TYPE_INT32, 4, null, null);
        sib += 4 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f6", ProductData.TYPE_UINT32, 4, null, null);
        sib += 4 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f7", ProductData.TYPE_FLOAT32, 4, null, null);
        sib += 4 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f8", ProductData.TYPE_FLOAT64, 4, null, null);
        sib += 8 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f9", ProductData.TYPE_UTC, 4, null, null);
        sib += 12 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
        recordInfo.add("f10", ProductData.TYPE_ASCII, 4, null, null);
        sib += 1 * 4;
        assertEquals(sib, recordInfo.getSizeInBytes());
    }

    public void testCreateRecord() {
        RecordInfo recordInfo = new RecordInfo("test");
        recordInfo.add("f1", ProductData.TYPE_INT8, 4, null, null);
        recordInfo.add("f2", ProductData.TYPE_UINT8, 4, null, null);
        recordInfo.add("f3", ProductData.TYPE_INT16, 4, null, null);
        recordInfo.add("f4", ProductData.TYPE_UINT16, 4, null, null);
        recordInfo.add("f5", ProductData.TYPE_INT32, 4, null, null);
        recordInfo.add("f6", ProductData.TYPE_UINT32, 4, null, null);
        recordInfo.add("f7", ProductData.TYPE_FLOAT32, 4, null, null);
        recordInfo.add("f8", ProductData.TYPE_FLOAT64, 4, null, null);
        recordInfo.add("f9", ProductData.TYPE_UTC, 4, null, null);
        recordInfo.add("f10", ProductData.TYPE_ASCII, 4, null, null);
        Record record = recordInfo.createRecord();
        assertSame(recordInfo, record.getInfo());
        assertEquals(10, record.getNumFields());
        assertEquals(4 * (1 + 1 + 2 + 2 + 4 + 4 + 4 + 8 + 12 + 1), record.getSizeInBytes());
    }

}
