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
package org.esa.s1tbx.io.radarsat2;

import org.esa.s1tbx.S1TBXTests;
import org.esa.s1tbx.TestData;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.datamodel.Product;
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
public class TestRadarsat2ProductReader {

    static {
        TestUtils.initTestEnvironment();
    }
    private Radarsat2ProductReaderPlugIn readerPlugin;
    private ProductReader reader;

    public TestRadarsat2ProductReader() {
        readerPlugin = new Radarsat2ProductReaderPlugIn();
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
        testProcessor.recurseReadFolder(this, S1TBXTests.rootPathsRadarsat2, readerPlugin, reader, null, null);
    }

    @Test
    public void testOpeningFolder() throws Exception {
        final File inputFile = TestData.inputRS2_SQuad;
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
        final File inputFile = TestData.inputRS2_SQuad;
        if(!inputFile.exists()){
            TestUtils.skipTest(this, inputFile +" not found");
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

/*    @Test
    public void testOpeningInputStream() throws Exception {
        final File inputFile = new File(rs2ZipFilePath);
        if(!inputFile.exists()){
            TestUtils.skipTest(this);
            return;
        }

        final InputStream inputStream = new FileInputStream(rs2ZipFilePath);

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputStream);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputStream, null);
        Assert.assertTrue(product != null);
    }*/
}
