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
package org.esa.s1tbx.io.terrasarx;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
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
public class TestTanDEMXProductReader extends ReaderTest {

    private final static File metadataFile = new File(S1TBXTests.inputPathProperty + S1TBXTests.sep +"SAR/TanDEM-X/HintonTDX_20110829/TDM1_SAR__COS_BIST_SM_S_SRA_20110829T013013_20110829T013021.xml");
    private final static String inputTanDEMX = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR/TanDEM-X/";
    private final static File[] rootPathsTanDEMX = S1TBXTests.loadFilePath(inputTanDEMX);

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        for (File file : rootPathsTanDEMX) {
            assumeTrue(file + " not found", file.exists());
        }
    }

    public TestTanDEMXProductReader() {
        super(new TerraSarXProductReaderPlugIn());
    }

    @Test
    public void testOpenMetadata() throws Exception {
        Product prod = testReader(metadataFile.toPath());
        validateProduct(prod);
        validateMetadata(prod);
        validateBands(prod, new String[] {
                "i_HH_mst_29Aug2011","q_HH_mst_29Aug2011","Intensity_HH_mst_29Aug2011",
                "i_HH_slv1_29Aug2011","q_HH_slv1_29Aug2011","Intensity_HH_slv1_29Aug2011"});
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, rootPathsTanDEMX, readerPlugIn, reader, null, null);
    }
}
