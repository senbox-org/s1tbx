/*
 * $Id: VirtualBandTest.java,v 1.2 2007/03/19 15:52:28 marcop Exp $
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

package org.esa.beam.framework.datamodel;

import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestSuite;
import com.bc.ceres.core.ProgressMonitor;

public class VirtualBandTest extends AbstractRasterDataNodeTest {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(VirtualBandTest.class);
    }

    public VirtualBandTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(VirtualBandTest.class);
    }

    protected void setUp() {
    }

    protected void tearDown() {
    }

    public void testExprAndTerm() {
        final Product product = new Product("p", "t", 10, 10);
        final VirtualBand virtualBand = new VirtualBand("vb", ProductData.TYPE_FLOAT32, 10, 10, "1.0");
        product.addBand(virtualBand);
        assertEquals("1.0", virtualBand.getExpression());
        try {
            virtualBand.readRasterDataFully(ProgressMonitor.NULL);
        } catch (IOException e) {
            fail("IOException not expected");
        }
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                assertEquals(1.0f, virtualBand.getPixelFloat(x, y), 1e-6f);
            }
        }
    }

    public void testReplaceExpressionIdentifier() {
        final String oldIdentifier = "oldIdentifier";
        final String newIdentifier = "newIdentifier";
        final String initialExpression = "identifier_1 + oldIdentifier - identifier_3";
        final String renamedExpression = "identifier_1 + newIdentifier - identifier_3";
        final VirtualBand virtualBand = new VirtualBand("vb", ProductData.TYPE_UINT16, 10, 10, initialExpression);

        final boolean[] isActiv = new boolean[]{false};
        final Product product = new Product("prod", "NO_TYPE", 10, 10) {

            protected void fireNodeChanged(ProductNode sourceNode, String propertyName, Object oldValue) {
                if (isActiv[0] && !propertyName.equalsIgnoreCase(Product.PROPERTY_NAME_MODIFIED)) {
                    fail("Event '" + propertyName + "' not expected");
                }
            }

            protected void fireNodeAdded(ProductNode sourceNode) {
                if (isActiv[0]) {
                    fail("Event not expected");
                }
            }

            protected void fireNodeDataChanged(DataNode sourceNode) {
                if (isActiv[0]) {
                    fail("Event not expected");
                }
            }

            protected void fireNodeRemoved(ProductNode sourceNode) {
                if (isActiv[0]) {
                    fail("Event not expected");
                }
            }

        };
        product.addBand(virtualBand);
        product.setModified(false);
        assertFalse(virtualBand.isModified());
        assertEquals(initialExpression, virtualBand.getExpression());

        isActiv[0] = true;
        virtualBand.updateExpression(oldIdentifier, newIdentifier);
        assertEquals(renamedExpression, virtualBand.getExpression());
        assertTrue(virtualBand.isModified());
    }

    protected RasterDataNode createRasterDataNode() {
        return new VirtualBand("vb", ProductData.TYPE_UINT16, 10, 10, "0");
    }
}
