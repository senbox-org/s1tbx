/*
 * $Id: DimapProductWriterPlugInTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.dimap;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.GlobalTestConfig;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Product;

public class DimapProductWriterPlugInTest extends TestCase {

    private final static DimapProductWriterPlugIn _plugIn = new DimapProductWriterPlugIn();
    private ProductWriter _productWriter;

    public DimapProductWriterPlugInTest(String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(DimapProductWriterPlugInTest.class);
    }

    protected void setUp() {
        _productWriter = _plugIn.createWriterInstance();
    }

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
