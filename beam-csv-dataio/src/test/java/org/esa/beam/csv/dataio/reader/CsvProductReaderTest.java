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

package org.esa.beam.csv.dataio.reader;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.Raster;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReaderTest {

    private ProductReader reader;

    @Before
    public void setUp() throws Exception {
        reader = new CsvProductReader(new CsvProductReaderPlugIn());
    }

    @Test
    public void testReadProductNodes() throws Exception {
        final Product product = reader.readProductNodes(getClass().getResource("../simple_format_example.txt").getFile(), null);

        assertNotNull(product);
        assertEquals(3, product.getSceneRasterWidth());
        assertEquals(1, product.getSceneRasterHeight());
        final Band[] bands = product.getBands();
        assertEquals(4, bands.length);
        assertEquals("lat", bands[0].getName());
        assertEquals("lon", bands[1].getName());
        assertEquals("radiance_1", bands[2].getName());
        assertEquals("radiance_2", bands[3].getName());

        assertEquals(ProductData.TYPE_FLOAT32, bands[0].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, bands[1].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, bands[2].getDataType());
        assertEquals(ProductData.TYPE_FLOAT32, bands[3].getDataType());
    }

    @Test
    public void testReadBandRasterData() throws Exception {
        final Product product = reader.readProductNodes(getClass().getResource("../simple_format_example.txt").getFile(), null);
        final Band radiance1 = product.getBand("radiance_1");
        final Band radiance2 = product.getBand("radiance_2");

        final Raster radiance1Data = radiance1.getSourceImage().getData();
        final Raster radiance2Data = radiance2.getSourceImage().getData();

        assertEquals(3, radiance1Data.getDataBuffer().getSize());
        assertEquals(3, radiance2Data.getDataBuffer().getSize());

        assertEquals(Float.NaN, radiance1Data.getSampleFloat(0, 0, 0), 1.0E-6);
        assertEquals(13.4f, radiance2Data.getSampleFloat(0, 0, 0), 1.0E-6);

        assertEquals(18.3f, radiance1Data.getSampleFloat(1, 0, 0), 1.0E-6);
        assertEquals(2.4f, radiance2Data.getSampleFloat(1, 0, 0), 1.0E-6);

        assertEquals(10.5f, radiance1Data.getSampleFloat(2, 0, 0), 1.0E-6);
        assertEquals(10.6f, radiance2Data.getSampleFloat(2, 0, 0), 1.0E-6);
    }

    @Test
    public void testInvalidInput() throws Exception {
        try {
            ((CsvProductReader)reader).getProductData(null, new ProductData.UTC());
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().matches("Unsupported type.*"));
        }
    }

    @Test
    public void testGetDataType() throws Exception {
        assertEquals(ProductData.TYPE_ASCII, ((CsvProductReader)reader).getProductDataType(String.class));
        assertEquals(ProductData.TYPE_FLOAT32, ((CsvProductReader)reader).getProductDataType(Float.class));
        assertEquals(ProductData.TYPE_FLOAT64, ((CsvProductReader)reader).getProductDataType(Double.class));
        assertEquals(ProductData.TYPE_INT8, ((CsvProductReader)reader).getProductDataType(Byte.class));
    }

}
