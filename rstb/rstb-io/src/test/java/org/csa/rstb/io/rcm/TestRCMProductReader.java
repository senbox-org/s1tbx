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
public class TestRCMProductReader extends ReaderTest {

    private final static File inputGRDZip = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK21594_PK225278_4_QP15_20110624_225945_HH_VV_HV_VH_GRD.zip");
    private final static File inputGRDFolder = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK21594_PK225278_4_QP15_20110624_225945_HH_VV_HV_VH_GRD");
    private final static File inputGRDManifest = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK21594_PK225278_4_QP15_20110624_225945_HH_VV_HV_VH_GRD/manifest.safe");

    private final static File inputGRCFolder = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK21594_PK225278_6_QP15_20110624_225945_HH_VV_HV_VH_GRC");
    private final static File inputGRCManifest = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK21594_PK225278_6_QP15_20110624_225945_HH_VV_HV_VH_GRC/manifest.safe");

    //private final static File inputSLCZip = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK77686_PK688502_2_3M26_20130822_020936_HH_SLC.zip");
    private final static File inputSLCFolder = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK21594_PK225278_2_QP15_20110624_225945_HH_VV_HV_VH_SLC");
    private final static File inputSLCManifest = new File(S1TBXTests.TEST_ROOT +"RCM/QP/RCM1_OK21594_PK225278_2_QP15_20110624_225945_HH_VV_HV_VH_SLC/manifest.safe");

    private final static File inputCPSLC = new File(S1TBXTests.TEST_ROOT +"RCM/CP/RCM1_OK21594_PK225278_2_16MCP9_20110624_225945_CH_CV_SLC");
    private final static File inputCPGRC = new File(S1TBXTests.TEST_ROOT +"RCM/CP/RCM1_OK21594_PK225278_6_16MCP9_20110624_225945_CH_CV_GRC");
    private final static File inputCPGRD = new File(S1TBXTests.TEST_ROOT +"RCM/CP/RCM1_OK21594_PK225278_4_16MCP9_20110624_225945_CH_CV_GRD");
    private final static File inputCPGRCZip = new File(S1TBXTests.TEST_ROOT +"RCM/CP/RCM1_OK21594_PK225278_6_16MCP9_20110624_225945_CH_CV_GRC.zip");

    public TestRCMProductReader() {
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

        assumeTrue(inputSLCFolder + " not found", inputSLCFolder.exists());
        assumeTrue(inputSLCManifest + " not found", inputSLCManifest.exists());

        assumeTrue(inputCPSLC + " not found", inputCPSLC.exists());
        assumeTrue(inputCPGRC + " not found", inputCPGRC.exists());
        assumeTrue(inputCPGRD + " not found", inputCPGRD.exists());
        assumeTrue(inputCPGRCZip + " not found", inputCPGRCZip.exists());
    }

    @Test
    public void testOpeningGRDManifest() throws Exception {
        testReader(inputGRDManifest.toPath());
    }

    @Test
    @Ignore("failing test")
    public void testOpeningGRDZip() throws Exception {
        testReader(inputGRDZip.toPath());
    }

    @Test
    public void testOpeningGRDFolder() throws Exception {
        testReader(inputGRDFolder.toPath());
    }

    @Test
    public void testOpeningGRCManifest() throws Exception {
        testReader(inputGRCManifest.toPath());
    }

    @Test
    public void testOpeningGRCFolder() throws Exception {
        testReader(inputGRCFolder.toPath());
    }

    @Test
    public void testOpeningSLCManifest() throws Exception {
        testReader(inputSLCManifest.toPath());
    }

    @Test
    public void testOpeningSLCFolder() throws Exception {
        testReader(inputSLCFolder.toPath());
    }

    @Test
    public void testOpeningCPSLC() throws Exception {
        testReader(inputCPSLC.toPath());
    }

    @Test
    public void testOpeningCPGRC() throws Exception {
        testReader(inputCPGRC.toPath());
    }

    @Test
    public void testOpeningCPGRD() throws Exception {
        testReader(inputCPGRD.toPath());
    }

    @Test
    public void testOpeningCPGRCZip() throws Exception {
        testReader(inputCPGRCZip.toPath());
    }
}
