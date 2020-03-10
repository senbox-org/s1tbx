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

    private final static String sep = S1TBXTests.sep;
    private final static File inputZip = null;
    private static final File inputFolder = new File(S1TBXTests.inputPathProperty + sep + "SAR" + sep + "K5" + sep + "HDF" + sep + "K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D");
    //private final static File inputFolder = new File("E:\\data\\K5\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D");
    private final static File inputMetaXML = new File(S1TBXTests.inputPathProperty + sep +  "SAR" + sep + "K5" + sep + "HDF" + sep + "K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D" + sep + "K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D_Aux.xml");
    //private final static File inputMetaXML = new File("E:\\data\\K5\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D\\K5_20170125111222_000000_18823_A_UH28_HH_GTC_B_L1D_Aux.xml");

    public TestKompsat5ProductReader() {
        super(new Kompsat5ReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputFolder + " not found", inputFolder.exists());
        assumeTrue(inputMetaXML + " not found", inputMetaXML.exists());
    }

    @Test
    public void testOpeningFolder() throws Exception {
        Product prod = testReader(inputFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"GIM","Amplitude_HH","Intensity_HH"});
    }

    @Test
    public void testOpeningMetadata() throws Exception {
        Product prod = testReader(inputMetaXML.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"GIM", "Amplitude_HH","Intensity_HH"});
    }

//    @Test
//    public void testOpeningZip() throws Exception {
//        Product prod = testReader(inputZip);
//    validateProduct(prod);
//    validateMetadata(prod);
//    }
}
