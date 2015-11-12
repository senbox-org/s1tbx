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


public abstract class AbstractRasterDataNodeTest extends AbstractDataNodeTest {

    protected abstract RasterDataNode createRasterDataNode();

    public void testSetAndGetBandStatistics() {
        RasterDataNode rasterDataNode = createRasterDataNode();
        assertEquals(null, rasterDataNode.getImageInfo());
        final ImageInfo imageInfo = new ImageInfo(new ColorPaletteDef(0, 1));
        rasterDataNode.setImageInfo(imageInfo);
        assertSame(imageInfo, rasterDataNode.getImageInfo());
    }

    public void testValidMaskExpressionIsAdjustedIfNodeNameChanged() {
        final RasterDataNode rasterDataNode = createRasterDataNode();
        rasterDataNode.setValidPixelExpression("flagsBand.f1 || not flagsBand.f2");
        final Product product = new Product("p", "NoType",
                                            rasterDataNode.getRasterWidth(), rasterDataNode.getRasterHeight());
        addRasterDataNodeToProduct(product, rasterDataNode);

        final FlagCoding flagCoding = new FlagCoding("f");
        flagCoding.addFlag("f1", 0x01, "descr");
        flagCoding.addFlag("f2", 0x02, "descr");

        final Band flagsBand = product.addBand("flagsBand", ProductData.TYPE_INT8);
        flagsBand.setSampleCoding(flagCoding);
        product.getFlagCodingGroup().add(flagCoding);

        flagsBand.setName("flags");

        final String currentExpression = rasterDataNode.getValidPixelExpression();
        final String expectedExpression = "flags.f1 || not flags.f2";
        assertEquals("name is not changed", expectedExpression, currentExpression);
    }

    public void testUpdateExpression() {
        final String oldIdentifier = "oldIdent";
        final String newIdentifier = "newIdent";
        final String initialExpression = "ident_1 + oldIdent - ident_3";
        final String renamedExpression = "ident_1 + newIdent - ident_3";
        final RasterDataNode node = createRasterDataNode();
        node.setValidPixelExpression(initialExpression);
        final int width = node.getRasterWidth();
        final int height = node.getRasterHeight();
        final boolean[] isActive = {false};
        final Product product = new Product("n", "t", width, height) {
            @Override
            protected void fireNodeAdded(ProductNode childNode, ProductNodeGroup parentNode) {
                if (isActive[0]) {
                    fail("Event not expected.");
                }
            }

            @Override
            protected void fireNodeRemoved(ProductNode childNode, ProductNodeGroup parentNode) {
                if (isActive[0]) {
                    fail("Event not expected.");
                }
            }

            @Override
            protected void fireNodeDataChanged(DataNode sourceNode) {
                if (isActive[0]) {
                    fail("Event not expected.");
                }
            }

        };
        addRasterDataNodeToProduct(product, node);
        product.setModified(false);
        assertFalse(node.isModified());
        assertEquals(initialExpression, node.getValidPixelExpression());

        isActive[0] = true;
        node.updateExpression(oldIdentifier, newIdentifier);
        assertTrue(node.isModified());
        assertEquals(renamedExpression, node.getValidPixelExpression());
    }

    private static void addRasterDataNodeToProduct(final Product product, final RasterDataNode rasterDataNode) {
        if (rasterDataNode instanceof Band) {
            product.addBand((Band) rasterDataNode);
        } else if (rasterDataNode instanceof TiePointGrid) {
            product.addTiePointGrid((TiePointGrid) rasterDataNode);
        } else {
            fail("couldn't add RasterDataNode to product. Node is of unknown type.");
        }
    }
}
