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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;

/**
 * Unit test for SingleTileOperator.
 */
public class TestDataAnalysisOperator extends TestCase {

    private OperatorSpi spi;

    @Override
    protected void setUp() throws Exception {
        spi = new DataAnalysisOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    public void testSampleOperator()  throws Exception {

        Product sourceProduct = createTestProduct(4, 4);

        DataAnalysisOp op = (DataAnalysisOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct gets initialize to be executed
        Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);

        Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);

        op.dispose();
        
        // get statistics from metadata
        System.out.println();
        System.out.println("# of bands = " + op.getNumOfBands());
        System.out.println("min = " + op.getMin(0));
        System.out.println("max = " + op.getMax(0));
        System.out.println("mean = " + op.getMean(0));
        System.out.println("std = " + op.getStd(0));
        System.out.println("var = " + op.getVarCoef(0));
        System.out.println("enl = " + op.getENL(0));
        assertTrue(op.getNumOfBands() == 1);
        assertTrue(Double.compare(op.getMin(0), 1.0) == 0);
        assertTrue(Double.compare(op.getMax(0), 16.0) == 0);
        assertTrue(Double.compare(op.getMean(0), 8.5) == 0);
        assertTrue(Double.compare(op.getStd(0), 4.6097722286464435) == 0);
        assertTrue(Double.compare(op.getVarCoef(0), 0.8621574728675674) == 0);
        assertTrue(Double.compare(op.getENL(0), 1.3453237410071943) == 0);
    }

    private static Product createTestProduct(int w, int h) {

        Product testProduct = new Product("p", "t", w, h);

        // create a Band: band1
        Band band1 = testProduct.addBand("band1", ProductData.TYPE_INT32);
        int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band1.setData(ProductData.createInstance(intValues));

        // create SPH MetadataElement with attributes: sample_type and mds1_tx_rx_polar
        MetadataElement sph = new MetadataElement("SPH");
        sph.addAttribute(new MetadataAttribute("sample_type",
                ProductData.createInstance("DETECTED"), false));
        testProduct.getMetadataRoot().addElement(sph);
        return testProduct;
    }
}
