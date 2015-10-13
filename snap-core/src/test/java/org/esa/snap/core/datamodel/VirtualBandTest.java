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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;

import java.io.IOException;

public class VirtualBandTest extends AbstractRasterDataNodeTest {

    @Override
    protected void setUp() {
    }

    @Override
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

        final boolean[] isActive = new boolean[]{false};
        final Product product = new Product("prod", "NO_TYPE", 10, 10) {

            protected void fireNodeChanged(ProductNode sourceNode, String propertyName, Object oldValue) {
                if (isActive[0]) {
                    fail("Event '" + propertyName + "' not expected");
                }
            }

            @Override
            protected void fireNodeDataChanged(DataNode sourceNode) {
                if (isActive[0]) {
                    fail("Event not expected");
                }
            }

            @Override
            protected void fireNodeAdded(ProductNode childNode, ProductNodeGroup parentNode) {
                if (isActive[0]) {
                    fail("Event not expected");
                }
            }

            @Override
            protected void fireNodeRemoved(ProductNode childNode, ProductNodeGroup parentNode) {
                if (isActive[0]) {
                    fail("Event not expected");
                }
            }


        };
        product.addBand(virtualBand);
        product.setModified(false);
        assertFalse(virtualBand.isModified());
        assertEquals(initialExpression, virtualBand.getExpression());

        isActive[0] = true;
        virtualBand.updateExpression(oldIdentifier, newIdentifier);
        assertEquals(renamedExpression, virtualBand.getExpression());
        assertTrue(virtualBand.isModified());
    }

    @Override
    protected RasterDataNode createRasterDataNode() {
        return new VirtualBand("vb", ProductData.TYPE_UINT16, 10, 10, "0");
    }
}
