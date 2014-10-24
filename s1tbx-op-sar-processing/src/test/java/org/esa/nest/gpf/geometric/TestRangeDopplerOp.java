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
package org.esa.nest.gpf.geometric;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.dataio.dem.ElevationModel;
import org.esa.nest.dataio.dem.ElevationModelDescriptor;
import org.esa.nest.dataio.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
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

    private final static String inputPathWSM = TestUtils.rootPathTestProducts + "\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";
    private final static String expectedPathWSM = TestUtils.rootPathTestProducts + "\\expected\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977_TC.dim";

    private final static String inputPathIMS = TestUtils.rootPathTestProducts + "\\input\\ENVISAT-ASA_IMS_1PNDPA20050405_211952_000000162036_00115_16201_8523.dim";
    private final static String expectedPathIMS = TestUtils.rootPathTestProducts + "\\expected\\ENVISAT-ASA_IMS_1PNDPA20050405_211952_000000162036_00115_16201_8523_TC.dim";

    private final static String inputPathAPM = TestUtils.rootPathTestProducts + "\\input\\ASA_APM_1PNIPA20030327_091853_000000152015_00036_05601_5422.N1";
    private final static String expectedPathAPM = TestUtils.rootPathTestProducts + "\\expected\\ENVISAT-ASA_APM_1PNIPA20030327_091853_000000152015_00036_05601_5422.N1_TC.dim";

    private String[] productTypeExemptions = {"_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX"};
    private String[] exceptionExemptions = {"not supported", "not be map projected", "outside of SRTM valid area",
                                "Source product should first be deburst"};

    /**
     * Processes a WSM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
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

        final float[] expected = new float[] { 0.5932531952857971f,0.8568953275680542f,0.5849599242210388f };
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

        final float[] expected = new float[] { 0.04975456744432449f,0.13621896505355835f,0.07706855237483978f};
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), expected);
    }

    /**
     * Processes a APM product and compares it to processed product known to be correct
     *
     * @throws Exception general exception
     */
    @Test
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

        final float[] expected = new float[] { 0.08639660477638245f,0.08651735633611679f,0.10073450207710266f };
        TestUtils.comparePixels(targetProduct, targetProduct.getBandAt(0).getName(), 500, 500, expected);
    }

    @Test
    public void testProcessAllASAR() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsASAR, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllERS() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsERS, productTypeExemptions, exceptionExemptions);
    }

    @Test
    public void testProcessAllALOS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsALOS, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllRadarsat2() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsRadarsat2, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllTerraSARX() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsTerraSarX, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllCosmo() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsCosmoSkymed, null, exceptionExemptions);
    }

    @Test
    public void testProcessAllSentinel1() throws Exception {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathsSentinel1, null, exceptionExemptions);
    }
}
