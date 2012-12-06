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
package org.esa.nest.gpf.filtering;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.TestUtils;

import java.util.Arrays;

/**
 * Unit test for SpeckleFilterOperator.
 */
public class SpeckleFilterOperatorTest extends TestCase {

    private OperatorSpi spi;
    private final static String inputPathWSM =     TestUtils.rootPathExpectedProducts+"\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";
    private final static String expectedPathWSM =  TestUtils.rootPathExpectedProducts+"\\expected\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977_Spk.dim";

    private String[] productTypeExemptions = { "_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX" };

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new SpeckleFilterOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    /**
     * Tests Mean speckle filter with a 4-by-4 test product.
     * @throws Exception The exception.
     */
    public void testMeanFilter() throws Exception {
        final Product sourceProduct = createTestProduct(4, 4);

        final SpeckleFilterOp op = (SpeckleFilterOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetFilter("Mean");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        final float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        final float[] expectedValues = {2.6666667f, 3.3333333f, 4.3333335f, 5.0f, 5.3333335f, 6.0f, 7.0f, 7.6666667f,
                                  9.333333f, 10.0f, 11.0f, 11.666667f, 12.0f, 12.666667f, 13.666667f, 14.333333f};
        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    /**
     * Tests Median speckle filter with a 4-by-4 test product.
     * @throws Exception anything
     */
    public void testMedianFilter() throws Exception {
        final Product sourceProduct = createTestProduct(4, 4);

        final SpeckleFilterOp op = (SpeckleFilterOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetFilter("Median");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        final float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs
        final float[] expectedValues = {2.0f, 3.0f, 4.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f,
                                  9.0f, 10.0f, 11.0f, 12.0f, 13.0f, 13.0f, 14.0f, 15.0f};
        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    /**
     * Tests Frost speckle filter with a 4-by-4 test product.
     * @throws Exception anything
     */
    public void testFrostFilter() throws Exception {
        final Product sourceProduct = createTestProduct(4, 4);

        final SpeckleFilterOp op = (SpeckleFilterOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetFilter("Frost");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        final float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs
        final float[] expectedValues = {2.3268945f, 3.1592662f, 4.2424283f, 4.956943f, 5.289399f, 6.0f, 7.0f, 7.684779f,
                                  9.321723f, 10.0f, 11.0f, 11.673815f, 12.006711f, 12.675643f, 13.674353f, 14.34112f};
        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    /**
     * Tests Gamma speckle filter with a 4-by-4 test product.
     * @throws Exception anything
     */
    public void testGammaFilter() throws Exception {
        final Product sourceProduct = createTestProduct(4, 4);

        final SpeckleFilterOp op = (SpeckleFilterOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetFilter("Gamma Map");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        final float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs
        final float[] expectedValues = {2.6666667f, 3.3333333f, 4.3333335f, 5.0f, 5.3333335f, 6.0f, 7.0f, 7.6666665f,
                                  9.333333f, 10.0f, 11.0f, 11.666667f, 12.0f, 12.666667f, 13.666667f, 14.333333f};
        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    /**
     * Tests Lee speckle filter with a 4-by-4 test product.
     * @throws Exception anything
     */
    public void testLeeFilter() throws Exception {
        final Product sourceProduct = createTestProduct(4, 4);

        final SpeckleFilterOp op = (SpeckleFilterOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetFilter("Lee");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        final float[] floatValues = new float[16];
        band.readPixels(0, 0, 4, 4, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs
        final float[] expectedValues = {2.6666667f, 3.3333333f, 4.3333335f, 5.0f, 5.3333335f, 6.0f, 7.0f, 7.6666665f,
                                  9.333333f, 10.0f, 11.0f, 11.666667f, 12.0f, 12.666667f, 13.666667f, 14.333333f};

        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    /**
     * Tests refined Lee speckle filter with a 7-by-7 test product.
     * @throws Exception anything
     */
    public void testRefinedLeeFilter() throws Exception {
        final Product sourceProduct = createRefinedLeeTestProduct();

        final SpeckleFilterOp op = (SpeckleFilterOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.SetFilter("Refined Lee");

        // get targetProduct gets initialize to be executed
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels gets computeTiles to be executed
        final float[] floatValues = new float[49];
        band.readPixels(0, 0, 7, 7, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs
        final float[] expectedValues = {
                117.125f, 115.6f, 108.98584f, 115.59759f, 111.30918f, 67.772064f, 77.42791f,
                120.4f, 115.62569f, 107.23333f, 99.46982f, 102.80414f, 83.69684f, 79.332756f,
                117.458336f, 115.96667f, 106.94328f, 122.64238f, 95.61863f, 80.35871f, 83.98103f,
                118.21429f, 116.314285f, 108.13215f, 103.94042f, 97.82989f, 76.39517f, 85.19781f,
                118.5f, 116.052475f, 105.69444f, 118.22289f, 103.49728f, 76.28018f, 81.13954f,
                120.008255f, 114.69612f, 106.46667f, 106.02027f, 98.75016f, 78.02842f, 85.9f,
                121.4375f, 119.85731f, 107.083336f, 106.53835f, 97.693275f, 88.09068f, 83.8125f
        };

        assertTrue(Arrays.equals(expectedValues, floatValues));
    }

    /**
     * Creates a 4-by-4 test product as shown below for speckle filter tests:
     *  1  2  3  4
     *  5  6  7  8
     *  9 10 11 12
     * 13 14 15 16
     * @param w width
     * @param h height
     * @return the new test product
     */
    private static Product createTestProduct(int w, int h) {
        final Product testProduct = TestUtils.createProduct("type", w, h);
        final Band band1 = testProduct.addBand("band1", ProductData.TYPE_INT32);
        final int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band1.setData(ProductData.createInstance(intValues));
        band1.setUnit(Unit.AMPLITUDE);
        return testProduct;
    }

    private static Product createRefinedLeeTestProduct() {
        int w = 7;
        int h = 7;
        final Product testProduct = TestUtils.createProduct("type", w, h);
        final Band band1 = testProduct.addBand("band1", ProductData.TYPE_INT32);
        final int[] intValues = { 99, 105, 124, 138, 128, 34, 62,
                                 105,  91, 140,  98, 114, 63, 31,
                                 107,  94, 128, 138,  96, 61, 82,
                                 137, 129, 136, 105, 100, 55, 85,
                                 144, 145, 113, 132, 119, 39, 50,
                                 102,  97, 102, 110, 103, 34, 53,
                                 107, 146, 115, 123, 101, 76, 56};

        band1.setData(ProductData.createInstance(intValues));
        band1.setUnit(Unit.AMPLITUDE);
        return testProduct;
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testProcessing() throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPathWSM);

        final SpeckleFilterOp op = (SpeckleFilterOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        TestUtils.compareProducts(targetProduct, expectedPathWSM, null);
    }

    public void testProcessAllASAR() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, null);
    }

    public void testProcessAllERS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, null, null);
    }

    public void testProcessAllALOS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, null, null);
    }

    public void testProcessAllRadarsat2() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, null, null);
    }

    public void testProcessAllTerraSARX() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathTerraSarX, null, null);
    }

    public void testProcessAllCosmo() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathCosmoSkymed, null, null);
    }

    public void testProcessAllNestBox() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathMixProducts, productTypeExemptions, null);
    }
}
