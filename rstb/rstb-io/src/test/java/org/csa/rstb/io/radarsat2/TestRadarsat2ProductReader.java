/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.io.radarsat2;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestRadarsat2ProductReader extends ReaderTest {

    private static final File folderSLC = new File(S1TBXTests.TEST_ROOT +"RS2/RS2_OK76385_PK678063_DK606752_FQ2_20080415_143807_HH_VV_HV_VH_SLC");
    private static final File metadataSLC = new File(S1TBXTests.TEST_ROOT +"RS2/RS2_OK76385_PK678063_DK606752_FQ2_20080415_143807_HH_VV_HV_VH_SLC/product.xml");
    private static final File inputRS2_SQuadFile = TestData.inputRS2_SQuad;

    private static final File zipQP_SGX = new File(S1TBXTests.TEST_ROOT +"RS2/RS2_OK76385_PK678075_DK606764_FQ15_20080506_142542_HH_VV_HV_VH_SGX.zip");
    private static final File zipDP_SGF = new File(S1TBXTests.TEST_ROOT +"RS2/RS2_OK76385_PK678077_DK606766_S7_20081111_141314_HH_HV_SGF.zip");
    private static final File zipDP_SGX = new File(S1TBXTests.TEST_ROOT +"RS2/RS2_OK76385_PK678083_DK606772_S7_20081111_141314_HH_HV_SGX.zip");
    private static final File zipDP_SSG = new File(S1TBXTests.TEST_ROOT +"RS2/RS2_OK76397_PK678155_DK606835_S7_20081111_141314_HH_HV_SSG.zip");

    public final static String inputRS2 = S1TBXTests.inputPathProperty + "/SAR/RS2/";
    public final static File[] rootPathsRadarsat2 = S1TBXTests.loadFilePath(inputRS2);

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputRS2_SQuadFile + " not found", inputRS2_SQuadFile.exists());
        assumeTrue(folderSLC + " not found", folderSLC.exists());
        assumeTrue(metadataSLC + " not found", metadataSLC.exists());
        assumeTrue(zipQP_SGX + " not found", zipQP_SGX.exists());
        assumeTrue(zipDP_SGF + " not found", zipDP_SGF.exists());
        assumeTrue(zipDP_SGX + " not found", zipDP_SGX.exists());
        assumeTrue(zipDP_SSG + " not found", zipDP_SSG.exists());

        for (File file : rootPathsRadarsat2) {
            assumeTrue(file + " not found", file.exists());
        }
    }

    public TestRadarsat2ProductReader() {
        super(new Radarsat2ProductReaderPlugIn());
    }

    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, rootPathsRadarsat2, readerPlugIn, reader, null, null);
    }

    @Test
    public void testOpeningFolder() throws Exception {
        Product prod = testReader(folderSLC.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "i_HH","q_HH","Intensity_HH",
                "i_HV","q_HV","Intensity_HV",
                "i_VV","q_VV","Intensity_VV",
                "i_VH","q_VH","Intensity_VH"});
    }

    @Test
    public void testOpeningMetadataFile() throws Exception {
        Product prod = testReader(metadataSLC.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "i_HH","q_HH","Intensity_HH",
                "i_HV","q_HV","Intensity_HV",
                "i_VV","q_VV","Intensity_VV",
                "i_VH","q_VH","Intensity_VH"});
    }

    @Test
    public void testOpeningQP_SGX_Zip() throws Exception {
        Product prod = testReader(zipQP_SGX.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "Amplitude_HH","Intensity_HH",
                "Amplitude_HV","Intensity_HV",
                "Amplitude_VV","Intensity_VV",
                "Amplitude_VH","Intensity_VH"});
    }

    @Test
    public void testOpeningDP_SGFZip() throws Exception {
        Product prod = testReader(zipDP_SGF.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "Amplitude_HH","Intensity_HH",
                "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningDP_SGXZip() throws Exception {
        Product prod = testReader(zipDP_SGX.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "Amplitude_HH","Intensity_HH",
                "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningDP_SSGZip() throws Exception {
        Product prod = testReader(zipDP_SSG.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "Amplitude_HH","Intensity_HH",
                "Amplitude_HV","Intensity_HV"});
    }
}
