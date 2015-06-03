/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.geometric;

import org.esa.s1tbx.S1TBXTests;
import org.esa.s1tbx.TestData;
import org.esa.snap.framework.dataio.ProductIO;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.dataop.dem.ElevationModel;
import org.esa.snap.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.framework.dataop.dem.ElevationModelRegistry;
import org.esa.snap.framework.dataop.resamp.ResamplingFactory;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.gpf.TestProcessor;
import org.esa.snap.util.TestUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;

/**
 * Unit test for Range Doppler.
 */
public class TestRangeDopplerOp {

    static {
        TestUtils.initTestEnvironment();
    }
    private final static OperatorSpi spi = new RangeDopplerGeocodingOp.Spi();
    private final static TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX"};
    private String[] exceptionExemptions = {"not supported", "not be map projected", "outside of SRTM valid area",
                                "Source product should first be deburst"};

    /**
     * Processes a WSM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    @Ignore("fails")
    public void testProcessWSM() throws Exception {
        final File inputFile = TestData.inputASAR_WSM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final RangeDopplerGeocodingOp op = (RangeDopplerGeocodingOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setApplyRadiometricCalibration(true);
        String[] bandNames = {"Amplitude"};
        op.setSourceBandNames(bandNames);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final float[] expected = new float[] { 0.211352f, 0.2079482f, 0.24199635f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), 500, 500, expected);
    }

    @Test
    public void testGetLocalDEM() throws Exception {

        final File inputFile = TestData.inputASAR_IMM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile+" not found");
            return;
        }

        final ProductReader reader = ProductIO.getProductReaderForInput(inputFile);
        final Product sourceProduct = reader.readProductNodes(inputFile, null);

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor("SRTM 3Sec");
        final ElevationModel dem = demDescriptor.createDem(ResamplingFactory.createResampling(ResamplingFactory.BILINEAR_INTERPOLATION_NAME));
        final GeoCoding targetGeoCoding = sourceProduct.getGeoCoding();

        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();

        final GeoPos geoPos = new GeoPos();
        final PixelPos pixPos = new PixelPos();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixPos.setLocation(x, y);
                targetGeoCoding.getGeoPos(pixPos, geoPos);
                dem.getElevation(geoPos);
            }
        }
    }

    /**
     * Processes a IMS product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    @Ignore("fails")
    public void testProcessIMS() throws Exception {
        final File inputFile = TestData.inputASAR_IMS;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final RangeDopplerGeocodingOp op = (RangeDopplerGeocodingOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setApplyRadiometricCalibration(true);
        String[] bandNames = {"i", "q"};
        op.setSourceBandNames(bandNames);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true, true);

        final float[] expected = new float[] { 0.010027291f, 0.13246238f, 0.12623858f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), expected);
    }

    /**
     * Processes a APM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
    @Ignore("fails")
    public void testProcessAPM() throws Exception {
        final File inputFile = TestData.inputASAR_APM;
        if (!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile + " not found");
            return;
        }
        final Product sourceProduct = TestUtils.readSourceProduct(inputFile);

        final RangeDopplerGeocodingOp op = (RangeDopplerGeocodingOp) spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setApplyRadiometricCalibration(true);
        String[] bandNames = {sourceProduct.getBandAt(0).getName()};
        op.setSourceBandNames(bandNames);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, true, true);

        final float[] expected = new float[] { 0.08069037f, 0.076275185f, 0.08034709f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), 500, 500, expected);
    }

    @Test
    public void testProcessAllASAR() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsASAR, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsERS, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllALOS() throws Exception
    {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsALOS, "ALOS PALSAR CEOS", null, exceptionExemptions);
    }

    @Test
    public void testProcessAllRadarsat2() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsRadarsat2, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllTerraSARX() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsTerraSarX, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllCosmo() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsCosmoSkymed, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsSentinel1, null, exceptionExemptions);
    }
}
