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
package org.esa.s1tbx.stac;

import org.esa.s1tbx.commons.test.ReaderTest;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assume.assumeTrue;

@Ignore
public class TestSTACWriter extends ReaderTest {

    private final static File input_PS_Tif = new File(S1TBXTests.inputPathProperty + "/SkyWatch_Internal/Planet/PlanetScope/Tif/20200601_160354_34_105d_3B_AnalyticMS.tif");

    private final static GeoTiffProductReaderPlugIn geoTiffReaderPlugin = new GeoTiffProductReaderPlugIn();

    @Before
    public void setUp() {
        // If the file does not exist: the test will be ignored
        assumeTrue(input_PS_Tif + " not found", input_PS_Tif.exists());
    }

    public TestSTACWriter() {
        super(geoTiffReaderPlugin);
    }

    @Test
    public void testWriteProduct_PS_to_GeoTiff() throws Exception {
        final ProductReader reader = readerPlugIn.createReaderInstance();
        final Product srcProduct = reader.readProductNodes(input_PS_Tif, null);

        final File folder = new File("c:\\out\\results\\Stac\\PS\\GeoTiff");
        final File outputFile = new File(folder, srcProduct.getName() + ".tif");

        ProductIO.writeProduct(srcProduct, outputFile, STACProductConstants.FORMAT, false);

        testReader(folder.toPath(), new STACProductReaderPlugIn());
    }
}