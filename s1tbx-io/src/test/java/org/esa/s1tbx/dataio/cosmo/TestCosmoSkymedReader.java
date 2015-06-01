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
package org.esa.s1tbx.dataio.cosmo;

import org.esa.s1tbx.S1TBXTests;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.gpf.TestProcessor;
import org.junit.Test;

/**
 * Test Product Reader.
 *
 * @author lveci
 */
public class TestCosmoSkymedReader {

    private CosmoSkymedReaderPlugIn readerPlugin;
    private ProductReader reader;

    private String[] exceptionExemptions = {"not supported"};

    public TestCosmoSkymedReader() {
        readerPlugin = new CosmoSkymedReaderPlugIn();
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
        testProcessor.recurseReadFolder(this, S1TBXTests.rootPathsCosmoSkymed, readerPlugin, reader, null, exceptionExemptions);
    }
}
