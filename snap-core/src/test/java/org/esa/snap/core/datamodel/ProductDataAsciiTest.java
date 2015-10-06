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

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import java.io.File;
import java.io.IOException;


public class ProductDataAsciiTest extends TestCase {

    private FileImageOutputStream _outputStream;
    private FileImageInputStream _inputStream;

    @Override
    protected void setUp() throws IOException {
        File outputFile = GlobalTestConfig.getBeamTestDataOutputFile("ProductData");
        outputFile.mkdirs();
        File streamFile = new File(outputFile, "ascii.img");
        streamFile.createNewFile();
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
        } catch (IOException e) {
        }
        FileUtils.deleteTree(GlobalTestConfig.getBeamTestDataOutputDirectory());
    }

    public void testDataTypeInconsistency() {
        ProductData instance = ProductData.createInstance(ProductData.TYPE_ASCII);
        assertEquals(ProductData.TYPE_ASCII, instance.getType());
    }

    public void testSingleValueConstructor() {
        ProductData instance = ProductData.createInstance(ProductData.TYPE_ASCII);
        instance.setElems(new byte[]{'#'});

        assertEquals(35, instance.getElemInt());
        assertEquals(35L, instance.getElemUInt());
        assertEquals(35.0F, instance.getElemFloat(), 0.0e-12F);
        assertEquals(35.0D, instance.getElemDouble(), 0.0e-12D);
        assertEquals("#", instance.getElemString());
        assertEquals(true, instance.getElemBoolean());
        assertEquals(1, instance.getNumElems());
        Object data = instance.getElems();
        assertEquals(true, data instanceof byte[]);
        assertEquals(1, ((byte[]) data).length);
        assertEquals(true, instance.isScalar());
        assertEquals(false, instance.isInt());
        assertEquals("#", instance.toString());

        ProductData expectedEqual = ProductData.createInstance(ProductData.TYPE_ASCII);
        expectedEqual.setElems(new byte[]{35});
        assertEquals(true, instance.equalElems(expectedEqual));

        ProductData expectedUnequal = ProductData.createInstance(ProductData.TYPE_ASCII);
        expectedUnequal.setElems(new byte[]{126});
        assertEquals(false, instance.equalElems(expectedUnequal));

//        StreamTest
        ProductData dataFromStream = null;
        try {
            instance.writeTo(_outputStream);
            dataFromStream = ProductData.createInstance(ProductData.TYPE_ASCII);
            dataFromStream.readFrom(_inputStream);
        } catch (IOException e) {
            fail("IOException not expected");
        }
        assertEquals(true, instance.equalElems(dataFromStream));
    }

    public void testConstructor() {
        ProductData instance = ProductData.createInstance(ProductData.TYPE_ASCII, 10);
        instance.setElems(new byte[]{'a', 'A', 'e', 'i', 'n', ' ', 'T', 'e', 's', 't'});

        assertEquals(97, instance.getElemIntAt(0));
        assertEquals(65, instance.getElemIntAt(1));
        assertEquals(101, instance.getElemIntAt(2));
        assertEquals(97L, instance.getElemUIntAt(0));
        assertEquals(65L, instance.getElemUIntAt(1));
        assertEquals(101L, instance.getElemUIntAt(2));
        assertEquals(97F, instance.getElemFloatAt(0), 0.0e-12F);
        assertEquals(65.0F, instance.getElemFloatAt(1), 0.0e-12F);
        assertEquals(101.0F, instance.getElemFloatAt(2), 0.0e-12F);
        assertEquals(97.0D, instance.getElemDoubleAt(0), 0.0e-12D);
        assertEquals(65.0D, instance.getElemDoubleAt(1), 0.0e-12D);
        assertEquals(101.0D, instance.getElemDoubleAt(2), 0.0e-12D);
        assertEquals("a", instance.getElemStringAt(0));
        assertEquals("A", instance.getElemStringAt(1));
        assertEquals("e", instance.getElemStringAt(2));
        assertEquals(10, instance.getNumElems());
        Object data2 = instance.getElems();
        assertEquals(true, data2 instanceof byte[]);
        assertEquals(10, ((byte[]) data2).length);
        assertEquals(false, instance.isScalar());
        assertEquals(false, instance.isInt());
        assertEquals("aAein Test", instance.toString());

        ProductData expectedEqual = ProductData.createInstance(ProductData.TYPE_ASCII, 10);
        expectedEqual.setElems(new byte[]{'a', 'A', 'e', 'i', 'n', ' ', 'T', 'e', 's', 't'});
        assertEquals(true, instance.equalElems(expectedEqual));

        ProductData expectedUnequal = ProductData.createInstance(ProductData.TYPE_ASCII, 10);
        expectedUnequal.setElems(new byte[]{'A', 'a', 'e', 'i', 'n', ' ', 'T', 'e', 's', 't'});
        assertEquals(false, instance.equalElems(expectedUnequal));

//        StreamTest
        ProductData dataFromStream = null;
        try {
            instance.writeTo(_outputStream);
            dataFromStream = ProductData.createInstance(ProductData.TYPE_ASCII, 10);
            dataFromStream.readFrom(_inputStream);
        } catch (IOException e) {
            fail("IOException not expected");
        }
        assertEquals(true, instance.equalElems(dataFromStream));
    }
}
