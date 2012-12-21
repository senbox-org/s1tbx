/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.ceos.alos;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.nest.util.TestUtils;

import java.io.File;

/**
 * Test ALOS PALSAR CEOS Product Reader.
 *
 * @author lveci
 */
public class TestAlosPalsarProductReader extends TestCase {

    private AlosPalsarProductReaderPlugIn readerPlugin;
    private ProductReader reader;

    private String[] exceptionExemptions = { "geocoding is null" };

    public TestAlosPalsarProductReader(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        readerPlugin = new AlosPalsarProductReaderPlugIn();
        reader = readerPlugin.createReaderInstance();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        reader = null;
        readerPlugin = null;
    }

    /**
     * Open all files in a folder recursively
     * @throws Exception anything
     */
    public void testOpenAll() throws Exception
    {
        final File folder = new File(TestUtils.rootPathALOS);
        if(!folder.exists()) {
            TestUtils.skipTest(this);
            return;
        }

        if(TestUtils.canTestReadersOnAllProducts)
            TestUtils.recurseReadFolder(folder, readerPlugin, reader, null, exceptionExemptions);
    }
}