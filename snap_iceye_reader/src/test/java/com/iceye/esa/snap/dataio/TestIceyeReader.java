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
package com.iceye.esa.snap.dataio;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;


/**
 * @author Ahmad Hamouda
 */
public class TestIceyeReader extends ReaderTest {

    public static final String TESTING_IMAGE_PATH = "/home/ahmad/Documents/Projects/snap/test";

    private final static String inputIceyeFolder = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR" + S1TBXTests.sep  + "Iceye" + S1TBXTests.sep ;
    private final static File[] iceyeSLCFiles = S1TBXTests.loadFilePath(inputIceyeFolder + "SLC");
    private final static File[] iceyeGRDFiles = S1TBXTests.loadFilePath(inputIceyeFolder + "GRD");

    private String[] exceptionExemptions = {"not supported"};

    public TestIceyeReader() {
        super(new IceyeProductReaderPlugIn());
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() {
        TestProcessor testProcessor = new TestProcessor(100, 100, 100, 100, 1, true, false);

        File file = new File(TESTING_IMAGE_PATH);
        File[] folderPaths = new File[1];
        folderPaths[0] = file;
        try {
            testProcessor.recurseReadFolder(this, folderPaths, readerPlugIn, reader, null, exceptionExemptions);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testTestServerOpenAllSLC() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, iceyeSLCFiles, readerPlugIn, null, null, null);
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testTestServerOpenAllGRD() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, iceyeGRDFiles, readerPlugIn, null, null, null);
    }
}
