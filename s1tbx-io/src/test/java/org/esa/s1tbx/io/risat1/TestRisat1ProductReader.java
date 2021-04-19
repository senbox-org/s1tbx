/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.io.risat1;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
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
public class TestRisat1ProductReader extends ReaderTest {

    private final static String sep = S1TBXTests.sep;

    private final static File inputCEOSFolder = new File(TestData.inputSAR  + sep +  "RISAT1/FRS-1/9441sd1_s33_GroundRange");
    private final static File inputCEOSMetaXML = new File(TestData.inputSAR + sep + "RISAT1/FRS-1/9441sd1_s33_GroundRange/BAND_META.txt");

    public TestRisat1ProductReader() {
        super(new Risat1ProductReaderPlugIn());
    }

    @Before
    public void setUp() {
        // If any of the file does not exist: the test will be ignored
        assumeTrue(inputCEOSFolder + " not found", inputCEOSFolder.exists());
        assumeTrue(inputCEOSMetaXML + " not found", inputCEOSMetaXML.exists());
    }

    @Test
    public void testOpeningCEOSFolder() throws Exception {
        Product prod = testReader(inputCEOSFolder.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_RCH","Intensity_RCH","Amplitude_RCV","Intensity_RCV"});
    }

    @Test
    public void testOpeningCEOSMetadata() throws Exception {
        Product prod = testReader(inputCEOSMetaXML.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {"Amplitude_RCH","Intensity_RCH","Amplitude_RCV","Intensity_RCV"});
    }
}
