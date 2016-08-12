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
package org.esa.snap.core.datamodel;

import junit.framework.TestCase;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.util.io.FileUtils;
import org.junit.Assume;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.IOException;

public class ProductDataLongTest extends TestCase {

    private FileImageInputStream _inputStream;
    private FileImageOutputStream _outputStream;

    @Override
    protected void setUp() throws IOException {
        File outputDir = GlobalTestConfig.getBeamTestDataOutputFile("ProductData");
        Assume.assumeTrue(outputDir.mkdirs() || outputDir.exists());
        File streamFile = new File(outputDir, "long.img");
        Assume.assumeTrue(streamFile.createNewFile() || streamFile.exists());
        _inputStream = new FileImageInputStream(streamFile);
        _outputStream = new FileImageOutputStream(streamFile);
        assertNotNull(_inputStream);
        assertNotNull(_outputStream);
    }

    @Override
    protected void tearDown() {
        try {
            _inputStream.close();
            _outputStream.close();
        } catch (IOException ignored) {
        }
        FileUtils.deleteTree(GlobalTestConfig.getBeamTestDataOutputDirectory());
    }

    public void testSingleValueConstructor() {
        long testValue = 2147483652L;
        ProductData instance = ProductData.createInstance(ProductData.TYPE_INT64);
        instance.setElems(new long[]{testValue});

        assertEquals(ProductData.TYPE_INT64, instance.getType());
        assertEquals(2147483652L, instance.getElemLongAt(0));
        assertEquals(2147483652L, instance.getElemUInt());
        assertEquals(2147483652.0F, instance.getElemFloat(), 0.0e-12F);
        assertEquals(2147483652.0D, instance.getElemDouble(), 0.0e-12D);
        assertEquals("2147483652", instance.getElemString());
        assertEquals(true, instance.getElemBoolean());
        assertEquals(1, instance.getNumElems());
        Object data = instance.getElems();
        assertEquals(true, data instanceof long[]);
        assertEquals(1, ((long[]) data).length);
        assertEquals(true, instance.isScalar());
        assertEquals(true, instance.isInt());
        assertEquals("2147483652", instance.toString());

        ProductData expectedEqual = ProductData.createInstance(ProductData.TYPE_INT64);
        expectedEqual.setElems(new long[]{testValue});
        assertEquals(true, instance.equalElems(expectedEqual));

        ProductData expectedUnequal = ProductData.createInstance(ProductData.TYPE_INT64);
        expectedUnequal.setElems(new long[]{testValue - 1});
        assertEquals(false, instance.equalElems(expectedUnequal));

//        StreamTest
        ProductData dataFromStream = null;
        try {
            instance.writeTo(_outputStream);
            dataFromStream = ProductData.createInstance(ProductData.TYPE_INT64);
            dataFromStream.readFrom(_inputStream);
        } catch (IOException e) {
            fail("IOException not expected");
        }
        assertEquals(true, instance.equalElems(dataFromStream));
    }

    public void testConstructor() {
        long testValue = 2147483652L;
        ProductData instance = ProductData.createInstance(ProductData.TYPE_INT64, 3);
        instance.setElems(new long[]{-1, testValue, -1 * testValue});

        assertEquals(ProductData.TYPE_INT64, instance.getType());
        assertEquals(-1L, instance.getElemLongAt(0));
        assertEquals(2147483652L, instance.getElemLongAt(1));
        assertEquals(-2147483652L, instance.getElemLongAt(2));
        assertEquals(-1, instance.getElemIntAt(0));
        assertEquals(-1L, instance.getElemUIntAt(0));
        assertEquals(2147483652L, instance.getElemUIntAt(1));
        assertEquals(-2147483652L, instance.getElemUIntAt(2));
        assertEquals(-1.0F, instance.getElemFloatAt(0), 0.0e-12F);
        assertEquals(2147483652.0F, instance.getElemFloatAt(1), 0.0e-12F);
        assertEquals(-2147483652.0F, instance.getElemFloatAt(2), 0.0e-12F);
        assertEquals(-1.0D, instance.getElemDoubleAt(0), 0.0e-12D);
        assertEquals(2147483652.0D, instance.getElemDoubleAt(1), 0.0e-12D);
        assertEquals(-2147483652.0D, instance.getElemDoubleAt(2), 0.0e-12D);
        assertEquals("-1", instance.getElemStringAt(0));
        assertEquals("2147483652", instance.getElemStringAt(1));
        assertEquals("-2147483652", instance.getElemStringAt(2));
        assertEquals(true, instance.getElemBooleanAt(0));
        assertEquals(true, instance.getElemBooleanAt(1));
        assertEquals(true, instance.getElemBooleanAt(2));
        assertEquals(3, instance.getNumElems());
        Object data2 = instance.getElems();
        assertEquals(true, data2 instanceof long[]);
        assertEquals(3, ((long[]) data2).length);
        assertEquals(false, instance.isScalar());
        assertEquals(true, instance.isInt());
        assertEquals("-1,2147483652,-2147483652", instance.toString());

        ProductData expectedEqual = ProductData.createInstance(ProductData.TYPE_INT64, 3);
        expectedEqual.setElems(new long[]{-1, 2147483652L, -1 * 2147483652L});
        assertEquals(true, instance.equalElems(expectedEqual));

        ProductData expectedUnequal = ProductData.createInstance(ProductData.TYPE_INT64, 3);
        expectedUnequal.setElems(new long[]{-1, 2147483652L, -1 * 2147483647 + 5});
        assertEquals(false, instance.equalElems(expectedUnequal));

//        StreamTest
        ProductData dataFromStream = null;
        try {
            instance.writeTo(_outputStream);
            dataFromStream = ProductData.createInstance(ProductData.TYPE_INT64, 3);
            dataFromStream.readFrom(_inputStream);
        } catch (IOException e) {
            fail("IOException not expected");
        }
        assertEquals(true, instance.equalElems(dataFromStream));
    }

    public void testSetElemsAsString() {
        final ProductData pd = ProductData.createInstance(ProductData.TYPE_INT64, 3);
        pd.setElems(new String[]{
                String.valueOf(Long.MAX_VALUE),
                String.valueOf(Long.MIN_VALUE),
                String.valueOf(0),
        });

        assertEquals(Long.MAX_VALUE, pd.getElemLongAt(0));
        assertEquals(Long.MIN_VALUE, pd.getElemLongAt(1));
        assertEquals(0, pd.getElemLongAt(2));
    }

    public void testSetElemsAsString_OutOfRange() {
        final ProductData pd1 = ProductData.createInstance(ProductData.TYPE_INT64, 1);
        try {
            pd1.setElems(new String[]{String.valueOf("9223372036854775808")});
        } catch (Exception e) {
            assertEquals(NumberFormatException.class, e.getClass());
            assertEquals(true, e.getMessage().contains("9223372036854775808"));
        }

        final ProductData pd2 = ProductData.createInstance(ProductData.TYPE_INT64, 1);
        try {
            pd2.setElems(new String[]{String.valueOf("-9223372036854775809")});
        } catch (Exception e) {
            assertEquals(NumberFormatException.class, e.getClass());
            assertEquals(true, e.getMessage().contains("-9223372036854775809"));
        }
    }
}
