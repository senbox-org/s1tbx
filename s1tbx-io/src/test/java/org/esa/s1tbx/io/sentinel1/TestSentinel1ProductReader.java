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
package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
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
public class TestSentinel1ProductReader extends ReaderTest {

    private final static File inputS1_AnnotGRD = new File(TestData.inputSAR+"S1"+File.separator+"S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.SAFE");
    private final static File inputS1_AnnotGRDZip = new File(TestData.inputSAR+"S1"+File.separator+"S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.zip");
    private final static File inputS1_meta1GRD = new File(TestData.inputSAR+"S1"+File.separator+"bandless1"+File.separator+"manifest.safe");
    private final static File inputS1_meta2GRD = new File(TestData.inputSAR+"S1"+File.separator+"bandless2"+File.separator+"manifest.safe");

    private final static File inputGRDFolder = new File("E:\\data\\S1\\aws\\S1A_IW_GRDH_1SDV_20180719T002854_20180719T002919_022856_027A78_042A");

    private String[] productTypeExemptions = {"RAW","OCN"};

    private final static String inputS1 = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR" + S1TBXTests.sep  + "S1" + S1TBXTests.sep ;
    private final static File[] rootPathsSentinel1 = S1TBXTests.loadFilePath(inputS1);

    private final static File inputS1_GRDFile = TestData.inputS1_GRD;

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inputS1_GRDFile + " not found", inputS1_GRDFile.exists());

        for (File file : rootPathsSentinel1) {
            assumeTrue(file + " not found", file.exists());
        }
    }


    public TestSentinel1ProductReader() {
        super(new Sentinel1ProductReaderPlugIn());
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, rootPathsSentinel1, readerPlugIn, reader, productTypeExemptions, null);
    }

    @Test
    @Ignore("Unknown data.")
    public void testOpeningFile() throws Exception {
        testReader(new File(inputS1_AnnotGRD, "manifest.safe"));
    }

    @Test
    @Ignore("Unknown data.")
    public void testOpeningBandlessMetadataFile1() throws Exception {
        testReader(inputS1_meta1GRD);
    }

    @Test
    @Ignore("Unknown data.")
    public void testOpeningBandlessMetadataFile2() throws Exception {
        testReader(inputS1_meta2GRD);
    }

    @Test
    @Ignore("Unknown data.")
    public void testOpeningAnnotFolder() throws Exception {
        testReader(inputS1_AnnotGRD);
    }

    @Test
    public void testOpeningZip() throws Exception {
        testReader(inputS1_GRDFile);
    }

    @Test
    @Ignore("Unknown data.")
    public void testOpeningAnnotationProduct() throws Exception {
        testReader(inputS1_AnnotGRDZip);
    }

    @Test
    @Ignore("Unknown data.")
    public void testOpeningFolder() throws Exception {
        testReader(inputGRDFolder);
    }
}
