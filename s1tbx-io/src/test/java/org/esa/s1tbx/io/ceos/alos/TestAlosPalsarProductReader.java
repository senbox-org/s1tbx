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
package org.esa.s1tbx.io.ceos.alos;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

/**
 * Test ALOS PALSAR CEOS Product Reader.
 *
 * @author lveci
 */
public class TestAlosPalsarProductReader extends ReaderTest {

    private String[] exceptionExemptions = {"geocoding is null", "not supported"};

    private static File inputFile = TestData.inputALOS_Zip;

    public final static String inputALOS = S1TBXTests.inputPathProperty + S1TBXTests.sep + "SAR" + S1TBXTests.sep  + "ALOS" + S1TBXTests.sep ;
    public final static File[] rootPathsALOS = S1TBXTests.loadFilePath(inputALOS);


    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inputFile + " not found", inputFile.exists());

        for (File file : rootPathsALOS) {
            assumeTrue(file + " not found", file.exists());
        }
    }

    public TestAlosPalsarProductReader() {
        super(new AlosPalsarProductReaderPlugIn());
    }

    @Test
    public void testOpeningZip() throws Exception {

        final DecodeQualification canRead = readerPlugIn.getDecodeQualification(inputFile);
        Assert.assertTrue(canRead == DecodeQualification.INTENDED);

        final Product product = reader.readProductNodes(inputFile, null);
        Assert.assertTrue(product != null);
    }

    /**
     * Open all files in a folder recursively
     *
     * @throws Exception anything
     */
    @Test
    public void testOpenAll() throws Exception {
        TestProcessor testProcessor = S1TBXTests.createS1TBXTestProcessor();
        testProcessor.recurseReadFolder(this, rootPathsALOS, readerPlugIn, reader, null, exceptionExemptions);
    }
}
