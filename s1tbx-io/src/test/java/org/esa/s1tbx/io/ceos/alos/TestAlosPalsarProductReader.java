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

import org.esa.s1tbx.commons.S1TBXTests;
import org.esa.s1tbx.commons.TestData;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Test ALOS PALSAR CEOS Product Reader.
 *
 * @author lveci
 */
public class TestAlosPalsarProductReader {



    private AlosPalsarProductReaderPlugIn readerPlugin;
    private ProductReader reader;

    private String[] exceptionExemptions = {"geocoding is null", "not supported"};

    public TestAlosPalsarProductReader() {
        readerPlugin = new AlosPalsarProductReaderPlugIn();
        reader = readerPlugin.createReaderInstance();
    }

    @Test
    public void testOpeningZip() throws Exception {
        final File inputFile = TestData.inputALOS_Zip;
        if(!inputFile.exists()) {
            TestUtils.skipTest(this, inputFile +" not found");
            return;
        }

        final DecodeQualification canRead = readerPlugin.getDecodeQualification(inputFile);
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
        testProcessor.recurseReadFolder(this, S1TBXTests.rootPathsALOS, readerPlugin, reader, null, exceptionExemptions);
    }
}
