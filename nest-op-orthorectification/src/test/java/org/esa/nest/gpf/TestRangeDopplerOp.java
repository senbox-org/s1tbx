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

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.nest.util.TestUtils;

import java.io.File;

/**
 * Unit test for Range Doppler.
 */
public class TestRangeDopplerOp extends TestCase {

    private OperatorSpi spi;
    private final static String inputPathWSM =     TestUtils.rootPathExpectedProducts+"\\input\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977.dim";
    private final static String expectedPathWSM =  TestUtils.rootPathExpectedProducts+"\\expected\\subset_1_of_ENVISAT-ASA_WSM_1PNPDE20080119_093446_000000852065_00165_30780_2977_TC.dim";

    private final static String inputPathIMS =     TestUtils.rootPathExpectedProducts+"\\input\\ENVISAT-ASA_IMS_1PNDPA20050405_211952_000000162036_00115_16201_8523.dim";
    private final static String expectedPathIMS =  TestUtils.rootPathExpectedProducts+"\\expected\\ENVISAT-ASA_IMS_1PNDPA20050405_211952_000000162036_00115_16201_8523_TC.dim";

    private final static String inputPathAPM =     TestUtils.rootPathExpectedProducts+"\\input\\ASA_APM_1PNIPA20030327_091853_000000152015_00036_05601_5422.N1";
    private final static String expectedPathAPM =  TestUtils.rootPathExpectedProducts+"\\expected\\ENVISAT-ASA_APM_1PNIPA20030327_091853_000000152015_00036_05601_5422.N1_TC.dim";

    private String[] productTypeExemptions = { "_BP", "XCA", "WVW", "WVI", "WVS", "WSS", "DOR_VOR_AX" };
    private String[] exceptionExemptions = { "not supported", "already map projected" };

    @Override
    protected void setUp() throws Exception {
        spi = new RangeDopplerGeocodingOp.Spi();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(spi);
    }

    @Override
    protected void tearDown() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(spi);
    }

    /**
     * Processes a WSM product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testProcessWSM() throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPathWSM);

        final RangeDopplerGeocodingOp op = (RangeDopplerGeocodingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setApplyRadiometricCalibration(true);
        String[] bandNames = {"Amplitude"};
        op.setSourceBandNames(bandNames);
        
        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        TestUtils.compareProducts(targetProduct, expectedPathWSM, null);
    }

    public void testGetLocalDEM() throws Exception {

        final File inputFile = new File("P:\\nest\\nest\\ESA Data\\RADAR\\ASAR\\Image Mode Medium Resolution\\ASA_IMM_1PNIPA20080507_220932_000000502068_00230_32348_0581.N1");
        if(!inputFile.exists()) {
            TestUtils.skipTest(this);
            return;
        }

        final ProductReader reader = ProductIO.getProductReaderForFile(inputFile);
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
     * @throws Exception general exception
     */
    public void testProcessIMS() throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPathIMS);

        final RangeDopplerGeocodingOp op = (RangeDopplerGeocodingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setApplyRadiometricCalibration(true);
        String[] bandNames = {"i", "q"};
        op.setSourceBandNames(bandNames);
        
        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        TestUtils.compareProducts(targetProduct, expectedPathIMS, null);
    }

    /**
     * Processes a APM product and compares it to processed product known to be correct
     * @throws Exception general exception
     */
    public void testProcessAPM() throws Exception {

        final Product sourceProduct = TestUtils.readSourceProduct(inputPathAPM);

        final RangeDopplerGeocodingOp op = (RangeDopplerGeocodingOp)spi.createOperator();
        assertNotNull(op);
        op.setSourceProduct(sourceProduct);
        op.setApplyRadiometricCalibration(true);
        String[] bandNames = {sourceProduct.getBandAt(0).getName()};
        op.setSourceBandNames(bandNames);

        // get targetProduct: execute initialize()
        final Product targetProduct = op.getTargetProduct();
        TestUtils.verifyProduct(targetProduct, false, false);
        TestUtils.compareProducts(targetProduct, expectedPathAPM, null);
    }

    public void testProcessAllASAR() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathASAR, productTypeExemptions, exceptionExemptions);
    }

    public void testProcessAllERS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathERS, productTypeExemptions, exceptionExemptions);
    }

  /*  public void testProcessAllALOS() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathALOS, null, exceptionExemptions);
    }     */

    public void testProcessAllRadarsat2() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathRadarsat2, null, exceptionExemptions);
    }

    public void testProcessAllTerraSARX() throws Exception
    {
        TestUtils.testProcessAllInPath(spi, TestUtils.rootPathTerraSarX, null, exceptionExemptions);
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
