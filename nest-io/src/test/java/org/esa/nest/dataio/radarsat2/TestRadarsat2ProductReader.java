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
package org.esa.nest.dataio.radarsat2;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.util.TestUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestRadarsat2ProductReader {

    private Radarsat2ProductReaderPlugIn readerPlugin;
    private ProductReader reader;

    final String rs2ZipFilePath = "J:\\Data\\zips\\RS2_OK22935_PK237493_DK219951_FQ5W_20111002_224256_HH_VV_HV_VH_SLC.zip";
    final String rs2ZipFilePath2 = "J:\\Data\\zips\\RS2_OK22935_PK237498_DK219956_FQ1W_20111102_223848_HH_VV_HV_VH_SLC2.zip";
    final String rs2FolderFilePath = "J:\\Data\\zips\\RS2_OK22935_PK237498_DK219956_FQ1W_20111102_223848_HH_VV_HV_VH_SLC";

    @Before
    public void setUp() throws Exception {
        TestUtils.initTestEnvironment();
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
        final File folder = new File(TestUtils.rootPathRadarsat2);
        if (!folder.exists()) {
            TestUtils.skipTest(this);
            return;
        }

        if (TestUtils.canTestReadersOnAllProducts)
            TestUtils.recurseReadFolder(folder, readerPlugin, reader, null, null);
    }

    @Test
    public void testOpeningFolder() throws Exception {
        final File inputFile = new File(rs2FolderFilePath, "product.xml");
        if(!inputFile.exists())
            TestUtils.skipTest(this);

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    @Test
    public void testOpeningZip() throws Exception {
        final File inputFile = new File(rs2ZipFilePath);
        if(!inputFile.exists())
            TestUtils.skipTest(this);

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    @Test
    public void testOpeningZip2() throws Exception {
        final File inputFile = new File(rs2ZipFilePath2);
        if(!inputFile.exists())
            TestUtils.skipTest(this);

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

/*    @Test
    public void testOpeningInputStream() throws Exception {
        final File inputFile = new File(rs2ZipFilePath);
        if(!inputFile.exists())
            TestUtils.skipTest(this);

        final InputStream inputStream = new FileInputStream(rs2ZipFilePath);

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputStream);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputStream, null);
        Assert.assertTrue(product != null);
    }*/
}