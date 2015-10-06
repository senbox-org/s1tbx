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

package org.esa.snap.csv.dataio.writer;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductWriterTest {

    private StringWriter result;
    private ProductWriter writer;
    private Product product;

    @Before
    public void setUp() throws Exception {
        result = new StringWriter();

        final CsvProductWriterPlugIn plugIn = new CsvProductWriterPlugIn(result, CsvProductWriter.WRITE_FEATURES |
                                                                                 CsvProductWriter.WRITE_PROPERTIES);
        writer = plugIn.createWriterInstance();
        product = new Product("testProduct", "testType", 2, 3);
        fillBandDataFloat(product.addBand("radiance_1", ProductData.TYPE_FLOAT32), 0);
        fillBandDataFloat(product.addBand("radiance_2", ProductData.TYPE_FLOAT64), 10);
        fillBandDataInt(product.addBand("radiance_3", ProductData.TYPE_INT32), 100);
        writer.writeProductNodes(product, "");
    }

    @Test
    public void testWriteHeader() throws Exception {
        StringWriter expected = getExpectedHeader();
        assertEquals(expected.toString(), result.toString().trim());
    }

    @Test
    public void testWriteFeatures() throws Exception {
        writer.writeBandRasterData(null, -1, -1, -1, -1, null, ProgressMonitor.NULL);
        final StringWriter expected = getExpectedHeader();
        expected.append("\n");
        expected.append("0\t0.0\t10.0\t100\n");
        expected.append("1\t1.0\t11.0\t101\n");
        expected.append("2\t2.0\t12.0\t102\n");
        expected.append("3\t3.0\t13.0\t103\n");
        expected.append("4\t4.0\t14.0\t104\n");
        expected.append("5\t5.0\t15.0\t105");
        assertEquals(expected.toString(), result.toString().trim());
    }

    private StringWriter getExpectedHeader() {
        final StringWriter expected = new StringWriter();
        expected.write("#sceneRasterWidth=2\n");
        expected.write("featureId\tradiance_1:float\tradiance_2:double\tradiance_3:int");
        return expected;
    }

    private void fillBandDataInt(Band band, int startValue) {
        final ProductData data = band.createCompatibleProductData(product.getSceneRasterWidth() * product.getSceneRasterHeight());
        int value = startValue;
        int dataIndex = 0;
        for(int i = 0; i < product.getSceneRasterWidth(); i++) {
            for(int j = 0; j < product.getSceneRasterHeight(); j++) {
                data.setElemIntAt(dataIndex++, value++);
            }
        }
        band.setData(data);
    }

    private void fillBandDataFloat(Band band, int startValue) {
        final ProductData data = band.createCompatibleProductData(product.getSceneRasterWidth() * product.getSceneRasterHeight());
        int value = startValue;
        int dataIndex = 0;
        for(int i = 0; i < product.getSceneRasterWidth(); i++) {
            for(int j = 0; j < product.getSceneRasterHeight(); j++) {
                data.setElemFloatAt(dataIndex++, value++);
            }
        }
        band.setData(data);
    }
}
