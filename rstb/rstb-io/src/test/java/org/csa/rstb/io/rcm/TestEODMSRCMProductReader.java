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
    }

    @Test
    public void testOpeningGRDManifest() throws Exception {
        Product prod = testReader(inputGRDManifest.toPath());
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

    @Test
    @Ignore("not implemented")
    public void testOpeningGRDZip() throws Exception {
        Product prod = testReader(inputGRDZip.toPath());
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningGRDFolder() throws Exception {
        Product prod = testReader(inputGRDFolder.toPath());
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningGRCManifest() throws Exception {
        Product prod = testReader(inputGRCManifest.toPath());
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }

    @Test
    public void testOpeningGRCFolder() throws Exception {
        Product prod = testReader(inputGRCFolder.toPath());
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH", "Amplitude_HV","Intensity_HV"});
    }
}
