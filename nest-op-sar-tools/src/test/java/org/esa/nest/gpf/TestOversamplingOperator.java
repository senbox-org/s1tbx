/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.TestUtils;

import java.util.Arrays;

/**
 * Unit test for OversamplingOperator.
 */
public class TestOversamplingOperator extends TestCase {

    private OperatorSpi spi;

    @Override
    protected void setUp() throws Exception {
        spi = new OversamplingOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    /**
     * Tests undersampling operator with a 6x12 "DETECTED" test product.
     * @throws Exception general exception
     */
    public void testOversampling() throws Exception {

        //final File file = new File("C:\\Data\\ASAR\\ASA_IMM_1P_0739.N1");
        //if(!file.exists()) return;
        
        //Product sourceProduct = ProductIO.readProduct(file, null);

        Product sourceProduct = createTestProduct(12, 6);

        OversamplingOp op = (OversamplingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);


        // get targetProduct: execute initialize()
        Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        float[] floatValues = new float[24];
        band.readPixels(0, 0, 24, 1, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        float[] expectedValues = {1.0f, 0.5090863f, 2.0f, 3.355916f, 3.0f, 3.0564091f, 4.0f, 4.8086267f, 5.0f,
                5.391582f, 6.0f, 6.519202f, 7.0f, 7.648014f, 8.0f, 8.232636f, 9.0f, 9.997277f, 10.0f, 9.694443f,
                11.0f, 13.105296f, 12.0f, 6.519202f};
        assertTrue(Arrays.equals(expectedValues, floatValues));
    }


    /**
     * Creates a 6-by-12 test product as shown below:
     *  1  2  3  4  5  6  7  8  9 10 11 12
     * 13 14 15 16 17 18 19 20 21 22 23 24
     * 25 26 27 28 29 30 31 32 33 34 35 36
     * 37 38 39 40 41 42 43 44 45 46 47 48
     * 49 50 51 52 53 54 55 56 57 58 59 60
     * 61 62 63 64 65 66 67 68 69 70 71 72
     * @param w width
     * @param h height
     * @return the created product
     */
    private static Product createTestProduct(int w, int h) {

        Product testProduct = TestUtils.createProduct("ASA_APG_1P", w, h);

        // create a Band: band1
        Band band1 = testProduct.addBand("band1", ProductData.TYPE_FLOAT32);
        band1.setUnit(Unit.AMPLITUDE);
        float[] intValues = new float[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1.0f;
        }
        band1.setData(ProductData.createInstance(intValues));

        // create abstracted metadata
        MetadataElement abs = AbstractMetadata.getAbstractedMetadata(testProduct);

        AbstractMetadata.setAttribute(abs, AbstractMetadata.PRODUCT_TYPE, "ASA_APG_1P");
        AbstractMetadata.setAttribute(abs, AbstractMetadata.SAMPLE_TYPE, "DETECTED");
        AbstractMetadata.setAttribute(abs, AbstractMetadata.range_spacing, 2.0F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.azimuth_spacing, 1.5F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.line_time_interval, 0.01F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.first_line_time,
                AbstractMetadata.parseUTC("10-MAY-2008 20:32:46.885684"));

        return testProduct;
    }
}