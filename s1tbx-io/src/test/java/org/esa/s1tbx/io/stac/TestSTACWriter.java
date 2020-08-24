/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.io.stac;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@Ignore
public class TestSTACWriter extends ReaderTest {

    private final static File inputGEOMeta = new File(S1TBXTests.inputPathProperty + "/SAR/Capella/Airborne/GEO/ARL_SM_GEO_HH_20190823162315_20190823162606_extended.json");

    private final static GeoTiffProductReaderPlugIn geoTiffReaderPlugin = new GeoTiffProductReaderPlugIn();

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(inputGEOMeta + " not found", inputGEOMeta.exists());
    }

    public TestSTACWriter() {
        super(geoTiffReaderPlugin);
    }

    @Test
    public void testWriteCapellaToStac() throws Exception {
        final Product srcProduct = ProductIO.readProduct(inputGEOMeta);
        assertNotNull(srcProduct);

        final File folder = new File("c:\\out\\results\\Stac");
        final File outputFile = new File(folder, srcProduct.getName() + ".json");

        ProductIO.writeProduct(srcProduct, outputFile, STACProductConstants.FORMAT, false);


    }
}