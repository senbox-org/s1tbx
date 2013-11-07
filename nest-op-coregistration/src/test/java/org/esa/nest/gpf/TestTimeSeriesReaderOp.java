/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for CreateStackOp.
 */
public class TestTimeSeriesReaderOp {

    private OperatorSpi spi;

    @Before
    public void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new TimeSeriesReaderOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Test
    public void testOperator() throws Exception {

        final TimeSeriesReaderOp op = (TimeSeriesReaderOp)spi.createOperator();

        final Product mstProduct = createTestProduct(40,40, 30, 10, 10, 20);
        final Product slvProduct1 = createTestProduct(40,40, 35, 15, 15, 25);

        op.setSourceProducts(new Product[] {mstProduct});
        op.setTestParameters(CreateStackOp.MASTER_EXTENT);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        final Band band = targetProduct.getBandAt(0);

        // readPixels gets computeTiles to be executed
        float[] floatValues = new float[1600];
        band.readPixels(0, 0, 40, 40, floatValues, ProgressMonitor.NULL);
    }

    private static Product createTestProduct(final int w, final int h,
                                             final float latTop, final float lonLeft,
                                             final float latBottom, final float lonRight) {

        final Product product = new Product("p", "ASA_IMP_1P", w, h);

        final Band band = product.addBand("amplitude", ProductData.TYPE_FLOAT32);
        band.setUnit(Unit.AMPLITUDE);
        float[] floatValues = new float[w * h];
        int i;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                i = y*w + x;
                floatValues[i] = 0;
            }
        }
        band.setData(ProductData.createInstance(floatValues));

        final float[] latCorners = new float[]{latTop, latTop, latBottom, latBottom};
        final float[] lonCorners = new float[]{lonLeft, lonRight, lonLeft, lonRight};

        ReaderUtils.addGeoCoding(product, latCorners, lonCorners);

        return product;
    }

}