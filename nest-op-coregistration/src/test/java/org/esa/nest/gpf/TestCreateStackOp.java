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
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.Unit;

/**
 * Unit test for CreateStackOp.
 */
public class TestCreateStackOp extends TestCase {

    private OperatorSpi spi;

    @Override
    protected void setUp() throws Exception {
        spi = new CreateStackOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    public void testOperator() throws Exception {

        final CreateStackOp op = (CreateStackOp)spi.createOperator();
        assertNotNull(op);

        final Product mstProduct = createTestProduct(40,40, 30, 10, 10, 20);
        final Product slvProduct1 = createTestProduct(40,40, 35, 15, 15, 25);

        //ProductIO.writeProduct(mstProduct, "c:\\data\\out\\mstProduct", "BEAM-DIMAP");
        //ProductIO.writeProduct(slvProduct1, "c:\\data\\out\\slvProduct1", "BEAM-DIMAP");

        op.setSourceProducts(new Product[] {mstProduct, slvProduct1});
        op.setTestParameters(CreateStackOp.MASTER_EXTENT);

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        float[] floatValues = new float[1600];
        band.readPixels(0, 0, 40, 40, floatValues, ProgressMonitor.NULL);

        //ProductIO.writeProduct(targetProduct, "c:\\data\\out\\targetProduct", "BEAM-DIMAP");
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