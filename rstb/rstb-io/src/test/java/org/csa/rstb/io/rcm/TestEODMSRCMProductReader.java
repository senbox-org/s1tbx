/*
 * Copyright (C) 2018 Skywatch. https://www.skywatch.com
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
package org.csa.rstb.io.rcm;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestEODMSRCMProductReader extends ReaderTest {

    private final static File inputGRDZip = new File(S1TBXTests.TEST_ROOT +"RCM/EODMS/GRD/RCM1_OK1024567_PK1025865_1_SC50MA_20191205_225938_HH_HV_GRD.zip");
    private final static File inputGRDFolder = new File(S1TBXTests.TEST_ROOT +"RCM/EODMS/GRD/RCM1_OK1024567_PK1025865_1_SC50MA_20191205_225938_HH_HV_GRD");
    private final static File inputGRDManifest = new File(S1TBXTests.TEST_ROOT +"RCM/EODMS/GRD/RCM1_OK1024567_PK1025865_1_SC50MA_20191205_225938_HH_HV_GRD/manifest.safe");

    private final static File inputGRCFolder = new File(S1TBXTests.TEST_ROOT +"RCM/EODMS/GCD/RCM2_OK1028884_PK1029284_2_16M12_20191214_111155_HH_HV_GCD");
    private final static File inputGRCManifest = new File(S1TBXTests.TEST_ROOT +"RCM/EODMS/GCD/RCM2_OK1028884_PK1029284_2_16M12_20191214_111155_HH_HV_GCD/manifest.safe");

    private final static File inputQP_SLC = new File(S1TBXTests.TEST_ROOT + "RCM/OpenData/Quebec City/RCM3_OK1050546_PK1050547_1_QP8_20191229_110339_HH_VV_HV_VH_SLC");

    private final static File inputCP_SLC = new File(S1TBXTests.TEST_ROOT + "RCM/OpenData/Winnipeg/RCM1_OK1050595_PK1051816_1_3MCP24_20200219_123901_CH_CV_SLC");
    private final static File inputCP_GCC = new File(S1TBXTests.TEST_ROOT + "RCM/OpenData/Winnipeg/RCM1_OK1052791_PK1052794_1_3MCP24_20200219_123901_CH_CV_GCC");
    private final static File inputCP_GRC = new File(S1TBXTests.TEST_ROOT + "RCM/OpenData/Winnipeg/RCM1_OK1052791_PK1052795_1_3MCP24_20200219_123901_CH_CV_GRC");
    private final static File inputCP_GRD = new File(S1TBXTests.TEST_ROOT + "RCM/OpenData/Winnipeg/RCM1_OK1052791_PK1052796_1_3MCP24_20200219_123901_CH_CV_GCD");

    public TestEODMSRCMProductReader() {
        super(new RCMProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputGRDZip + " not found", inputGRDZip.exists());
        assumeTrue(inputGRDFolder + " not found", inputGRDFolder.exists());
        assumeTrue(inputGRDManifest + " not found", inputGRDManifest.exists());

        assumeTrue(inputGRCFolder + " not found", inputGRCFolder.exists());
        assumeTrue(inputGRCManifest + " not found", inputGRCManifest.exists());

        assumeTrue(inputQP_SLC + " not found", inputQP_SLC.exists());
        assumeTrue(inputCP_SLC + " not found", inputCP_SLC.exists());
        assumeTrue(inputCP_GCC + " not found", inputCP_GCC.exists());
        assumeTrue(inputCP_GRC + " not found", inputCP_GRC.exists());
        assumeTrue(inputCP_GRD + " not found", inputCP_GRD.exists());
    }

    @Test
    public void testOpeningGRDManifest() throws Exception {
        Product prod = testReader(inputGRDManifest.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

//    @Test
//    @Ignore("not implemented")
//    public void testOpeningGRDZip() throws Exception {
//        Product prod = testReader(inputGRDZip.toPath());
//        validateProduct(prod);
//        validateMetadata(prod);
//        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
//    }

    @Test
    public void testOpeningGRDFolder() throws Exception {
        Product prod = testReader(inputGRDFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningGRCManifest() throws Exception {
        Product prod = testReader(inputGRCManifest.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningGRCFolder() throws Exception {
        Product prod = testReader(inputGRCFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningQP_SLC() throws Exception {
        Product prod = testReader(inputQP_SLC.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_HH","q_HH","Intensity_HH", "i_HV","q_HV","Intensity_HV","i_VV","q_VV","Intensity_VV", "i_VH","q_VH","Intensity_VH"});
    }

    @Test
    public void testOpeningCP_SLC() throws Exception {
        Product prod = testReader(inputCP_SLC.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_RCH","q_RCH","Intensity_RCH","i_RCV","q_RCV","Intensity_RCV"});
    }

    @Test
    public void testOpeningCP_GCC() throws Exception {
        Product prod = testReader(inputCP_GCC.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_RCH","Intensity_RCH","Amplitude_RCV","Intensity_RCV"});
    }

    @Test
    public void testOpeningCP_GRC() throws Exception {
        Product prod = testReader(inputCP_GRC.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"i_RCH","q_RCH","Intensity_RCH","i_RCV","q_RCV","Intensity_RCV"});
    }

    @Test
    public void testOpeningCP_GRD() throws Exception {
        Product prod = testReader(inputCP_GRD.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_RCH","Intensity_RCH", "Amplitude_RCV","Intensity_RCV"});
    }
}
