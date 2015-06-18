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

import org.esa.s1tbx.S1TBXTests;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.Product;
import org.esa.s1tbx.TestData;
import org.esa.snap.gpf.TestProcessor;
import org.esa.snap.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestSentinel1ProductReader {

    static {
        TestUtils.initTestEnvironment();
    }
    private Sentinel1ProductReaderPlugIn readerPlugin;
    private ProductReader reader;

    public final static File inputS1_AnnotGRD = new File(TestData.inputSAR+"S1"+File.separator+"S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.SAFE");
    public final static File inputS1_AnnotGRDZip = new File(TestData.inputSAR+"S1"+File.separator+"S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.zip");

    public TestSentinel1ProductReader() throws Exception {
        readerPlugin = new Sentinel1ProductReaderPlugIn();
        reader = readerPlugin.createReaderInstance();
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, S1TBXTests.rootPathsSentinel1, readerPlugin, reader, null, null);
    }

    @Test
    public void testOpeningFolder() throws Exception {
        final File inputFile = new File(inputS1_AnnotGRD, "manifest.safe");
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile +" not found");
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    @Test
    public void testOpeningZip() throws Exception {
        final File inputFile = TestData.inputS1_GRD;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile +" not found");
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    @Test
    public void testOpeningAnnotationProduct() throws Exception {
        final File inputFile = inputS1_AnnotGRDZip;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile +" not found");
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    @Test
    public void testOpeningZipAnnotationProduct() throws Exception {
        final File inputFile = TestData.inputS1_GRD;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile +" not found");
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

 /*   @Test
    public void testOpeningInputStream() throws Exception {
        final File inputFile = new File(s1ZipFilePath);
        if(!inputFile.exists()) {
            TestUtils.skipTest(this);
            return;
        }

        final InputStream inputStream = new FileInputStream(s1ZipFilePath);

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputStream);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputStream, null);
        Assert.assertTrue(product != null);
    }*/
}
