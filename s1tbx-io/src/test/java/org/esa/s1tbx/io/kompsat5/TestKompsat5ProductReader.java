/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.kompsat5;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestKompsat5ProductReader extends ReaderTest {

    private static final File inputHDF_GTCFolder = new File(S1TBXTests.inputPathProperty + "/SAR/K5/HDF/K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D");
    private final static File inputHDF_GTCMetaXML = new File(S1TBXTests.inputPathProperty + "/SAR/K5/HDF/K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D/K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D_Aux.xml");

    private static final File inputHDF_SCSFolder = new File(S1TBXTests.inputPathProperty + "/SAR/K5/HDF/K5_20170125111222_000000_18823_A_UH28_HH_SCS_B_L1A");
    private final static File inputHDF_SCSMetaXML = new File(S1TBXTests.inputPathProperty + "/SAR/K5/HDF/K5_20170125111222_000000_18823_A_UH28_HH_SCS_B_L1A/K5_20170125111222_000000_18823_A_UH28_HH_SCS_B_L1A_Aux.xml");

    private final static File inputGeoTiffFolder = new File(S1TBXTests.inputPathProperty + "/SAR/K5/GeoTiff/K5_20190215223304_000000_30122_A_UH27_HH_GEC_B_L1C");
    private final static File inputGeoTiffMetaXML = new File(S1TBXTests.inputPathProperty + "/SAR/K5/GeoTiff/K5_20190215223304_000000_30122_A_UH27_HH_GEC_B_L1C/K5_20190215223304_000000_30122_A_UH27_HH_GEC_B_L1C_Aux.xml");

    public TestKompsat5ProductReader() {
        super(new Kompsat5ReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputHDF_GTCMetaXML + " not found", inputHDF_GTCMetaXML.exists());
        assumeTrue(inputHDF_SCSMetaXML + " not found", inputHDF_SCSMetaXML.exists());
        assumeTrue(inputGeoTiffMetaXML + " not found", inputGeoTiffMetaXML.exists());
    }

    @Test
    public void testOpeningHDF_GTCFolder() throws Exception {
        Product prod = testReader(inputHDF_GTCFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"GIM","Amplitude_HH","Intensity_HH"});
    }

    @Test
    public void testOpeningHDF_GTCMetadata() throws Exception {
        Product prod = testReader(inputHDF_GTCMetaXML.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"GIM", "Amplitude_HH","Intensity_HH"});
    }

    @Test
    public void testOpeningHDF_SCSFolder() throws Exception {
        Product prod = testReader(inputHDF_SCSFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"GIM", "i_HH", "q_HH", "Intensity_HH"});
    }

    @Test
    public void testOpeningHDF_SCSMetadata() throws Exception {
        Product prod = testReader(inputHDF_SCSMetaXML.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"GIM", "i_HH", "q_HH", "Intensity_HH"});
    }

//    @Test
//    public void testOpeningZip() throws Exception {
//        Product prod = testReader(inputZip);
//    validateProduct(prod);
//    validateMetadata(prod);
//    }

    @Test
    public void testOpeningGeoTiffFolder() throws Exception {
        Product prod = testReader(inputGeoTiffFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH"});
    }

    @Test
    public void testOpeningGeotiffMetadata() throws Exception {
        Product prod = testReader(inputGeoTiffMetaXML.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH"});
    }
}
