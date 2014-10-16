/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.sentinel1;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.snap.util.TestData;
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

    final String s1ZipFilePath = TestUtils.rootPathTestProducts+"input\\S1\\S1A_S1_GRDM_1SDV_20140607T172812_20140607T172836_000947_000EBD_7543.zip";
    final String s1AnnotationProductPath = TestUtils.rootPathTestProducts+"input\\S1\\S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.SAFE\\manifest.safe";
    final String s1ZipAnnotationProductPath = TestUtils.rootPathTestProducts+"input\\S1\\S1A_IW_GRDH_1ADV_20140819T224528_20140819T224546_002015_001F3B_979A.zip";
    final String s1FolderFilePath = "P:\\s1tbx\\s1tbx\\Data\\First Images\\S1A_S1_SLC__1SDV_20140607T172812_20140607T172836_000947_000EBD_4DB2.SAFE";

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
        final File folder = new File(TestUtils.rootPathSentinel1);
        if (!folder.exists()) {
            TestUtils.skipTest(this);
            return;
        }

        if (TestUtils.canTestReadersOnAllProducts)
            TestUtils.recurseReadFolder(folder, readerPlugin, reader, null, null);
    }

    @Test
    public void testOpeningFolder() throws Exception {
        final File inputFile = new File(s1FolderFilePath, "manifest.safe");
        if(!inputFile.exists()) {
            TestUtils.skipTest(this);
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
            TestUtils.skipTest(this);
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    @Test
    public void testOpeningAnnotationProduct() throws Exception {
        final File inputFile = new File(s1AnnotationProductPath);
        if(!inputFile.exists()) {
            TestUtils.skipTest(this);
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    @Test
    public void testOpeningZipAnnotationProduct() throws Exception {
        final File inputFile = new File(s1ZipAnnotationProductPath);
        if(!inputFile.exists()) {
            TestUtils.skipTest(this);
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