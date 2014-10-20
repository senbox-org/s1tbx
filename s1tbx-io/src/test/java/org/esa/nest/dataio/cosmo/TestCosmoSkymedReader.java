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
package org.esa.nest.dataio.cosmo;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

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
        final File folder = new File(TestUtils.rootPathCosmoSkymed);
        if (TestUtils.canTestReadersOnAllProducts)
            TestUtils.recurseReadFolder(this, folder, readerPlugin, reader, null, exceptionExemptions);
    }
}