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
package org.esa.s1tbx.io.seasat;

import org.esa.s1tbx.commons.test.MetadataValidator;
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
public class TestSeaSatProductReader extends ReaderTest {

    private final static File metadataFile = new File(S1TBXTests.inputPathProperty + S1TBXTests.sep +"SAR/Seasat/SS_00263_STD_F0886_tif/SS_00263_STD_F0886.xml");
    private final static File zipFile = new File(S1TBXTests.inputPathProperty + S1TBXTests.sep +"SAR/Seasat/SS_00263_STD_F0886_tif.zip");

    final static MetadataValidator.ValidationOptions options = new MetadataValidator.ValidationOptions();

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(metadataFile + " not found", metadataFile.exists());

        options.validateSRGR = false;
    }

    public TestSeaSatProductReader() {
        super(new SeaSatProductReaderPlugIn());
    }

    @Test
    public void testOpenMetadata() throws Exception {
        Product prod = testReader(metadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod, options);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH"});
    }

    @Test
    public void testOpenZip() throws Exception {
        Product prod = testReader(zipFile.toPath());
        validateProduct(prod);
        validateMetadata(prod, options);
        validateBands(prod, new String[] {"Amplitude_HH","Intensity_HH"});
    }
}
