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
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.util.TestUtils;

import java.util.Arrays;

/**
 * Unit test for MultilookOperator.
 */
public class TestMultilookOperator extends TestCase {

    private OperatorSpi spi;

    private final static String inputPathWSM =     TestUtils.rootPathExpectedProducts+"\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";
    private final static String expectedPathWSM =  TestUtils.rootPathExpectedProducts+"\\expected\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977_ML.dim";

    private String[] productTypeExemptions = { "_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX" };
    private String[] exceptionExemptions = { "not supported", "not intended" };

    @Override
    protected void setUp() throws Exception {
        TestUtils.initTestEnvironment();
        spi = new MultilookOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    /**
     * Tests multi-look operator with a 4x16 "DETECTED" test product.
     * @throws Exception general exception
     */
    public void testMultilookOfRealImage() throws Exception {
        if(TestUtils.skipTest(this))
            return;

        final Product sourceProduct = createTestProduct(16, 4);

        final MultilookOp op = (MultilookOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setNumRangeLooks(4);
        MultilookOp.DerivedParams param = new MultilookOp.DerivedParams();
        param.nRgLooks = 4;
        op.getDerivedParameters(sourceProduct, param);
        op.setNumAzimuthLooks(param.nAzLooks);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final Band band = targetProduct.getBandAt(0);
        assertNotNull(band);

        // readPixels: execute computeTiles()
        final float[] floatValues = new float[8];
        band.readPixels(0, 0, 4, 2, floatValues, ProgressMonitor.NULL);

        // compare with expected outputs:
        final float[] expectedValues = {10.5f, 14.5f, 18.5f, 22.5f, 42.5f, 46.5f, 50.5f, 54.5f};
        assertTrue(Arrays.equals(expectedValues, floatValues));

        // compare updated metadata
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(targetProduct);

        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_looks, 2.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_looks, 4.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.azimuth_spacing, 4.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.range_spacing, 2.0);
        TestUtils.attributeEquals(abs, AbstractMetadata.line_time_interval, 0.02);
        TestUtils.attributeEquals(abs, AbstractMetadata.first_line_time, "10-MAY-2008 20:32:46.890683");
    }

    /**
     * Processes a product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testProcessing() throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPathWSM);
        
        final MultilookOp op = (MultilookOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        TestUtils.compareProducts(targetProduct, expectedPathWSM, null);
    }

    /**
     * Creates a 4-by-16 test product as shown below:
     *  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16
     * 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32
     * 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48
     * 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63 64
     * @param w width
     * @param h height
     * @return the created product
     */
    private static Product createTestProduct(final int w, final int h) {

        final Product testProduct = TestUtils.createProduct("ASA_APG_1P", w, h);

        // create a Band: band1
        final Band band1 = testProduct.addBand("band1", ProductData.TYPE_INT32);
        band1.setUnit(Unit.AMPLITUDE);
        final int[] intValues = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            intValues[i] = i + 1;
        }
        band1.setData(ProductData.createInstance(intValues));

        // create abstracted metadata
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(testProduct);

        AbstractMetadata.setAttribute(abs, AbstractMetadata.SAMPLE_TYPE, "DETECTED");
        AbstractMetadata.setAttribute(abs, AbstractMetadata.MISSION, "ENVISAT");
        AbstractMetadata.setAttribute(abs, AbstractMetadata.srgr_flag, 0);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.range_spacing, 0.5F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.azimuth_spacing, 2.0F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.azimuth_looks, 1);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.range_looks, 1);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.line_time_interval, 0.01F);
        AbstractMetadata.setAttribute(abs, AbstractMetadata.first_line_time,
                AbstractMetadata.parseUTC("10-MAY-2008 20:32:46.885684"));

        final float[] incidence_angle = new float[64];
        Arrays.fill(incidence_angle, 30.0f);
        testProduct.addTiePointGrid(new TiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE, 16, 4, 0, 0, 1, 1, incidence_angle));

        return testProduct;
    }

    public void testProcessAllASAR() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, null);
    }

    public void testProcessAllERS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, null);
    }

    public void testProcessAllALOS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, null, null);
    }

    public void testProcessAllRadarsat2() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, null, exceptionExemptions);
    }

    public void testProcessAllTerraSARX() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathTerraSarX, null, null);
    }

    public void testProcessAllCosmo() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathCosmoSkymed, null, exceptionExemptions);
    }

    public void testProcessAllNestBox() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathMixProducts, productTypeExemptions, exceptionExemptions);
    }
}
