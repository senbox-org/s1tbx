/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataio.dimap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.IOException;

public class DimapProductWriterPlugInTest extends TestCase {

    private final static DimapProductWriterPlugIn _plugIn = new DimapProductWriterPlugIn();
    private ProductWriter _productWriter;

    public DimapProductWriterPlugInTest(String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(DimapProductWriterPlugInTest.class);
    }

    @Override
    protected void setUp() {
        _productWriter = _plugIn.createWriterInstance();
    }

    @Override
    protected void tearDown() {
        _productWriter = null;
    }

    public void testPlugInInfoQuery() {

        assertNotNull(_plugIn.getFormatNames());
        assertEquals(1, _plugIn.getFormatNames().length);
        assertEquals(DimapProductConstants.DIMAP_FORMAT_NAME, _plugIn.getFormatNames()[0]);

        assertNotNull(_plugIn.getOutputTypes());
        assertEquals(2, _plugIn.getOutputTypes().length);

        assertNotNull(_plugIn.getDescription(null));
    }

    public void testCreatedProductWriterInstance() {
        assertNotNull(_productWriter);
        assertEquals(true, _productWriter instanceof DimapProductWriter);
    }

    public void testWriteProductNodes() {
        Product product = new Product("test", "TEST", 10, 10);
        File outputFile = GlobalTestConfig.getBeamTestDataOutputFile("DIMAP/test.dim");
        try {
            _productWriter.writeProductNodes(product, outputFile);
        } catch (IOException e) {
            fail("unexpected IOException: " + e.getMessage());
        }
    }

}
