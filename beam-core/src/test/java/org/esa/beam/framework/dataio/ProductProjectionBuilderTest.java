/*
 * $Id: ProductProjectionBuilderTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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
package org.esa.beam.framework.dataio;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PinSymbol;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformFactory;

import java.io.IOException;

public class ProductProjectionBuilderTest extends TestCase {

    private Product product;
    private ProductProjectionBuilder projectionBuilder;

    @Override
    protected void setUp() throws Exception {
        MapTransform transform = MapTransformFactory.createTransform("Affine", new double[]{1, 2, 3, 4, 5, 6});
        MapProjection projection = new MapProjection("mp", transform);
        MapInfo mapInfo = new MapInfo(projection, 3, 4, 5, 6, 7, 8, Datum.WGS_84);
        mapInfo.setSceneWidth(40);
        mapInfo.setSceneHeight(50);
        projectionBuilder = new ProductProjectionBuilder(mapInfo);

        product = new Product("p", "t", 3, 3);
        TiePointGrid t1 = new TiePointGrid("t1", 3, 3, 0, 0, 1, 1,
                                           new float[]{0.6f, 0.3f, 0.4f, 0.8f, 0.9f, 0.4f, 0.3f, 0.2f, 0.4f});
        product.addTiePointGrid(t1);
        TiePointGrid t2 = new TiePointGrid("t2", 3, 3, 0, 0, 1, 1,
                                           new float[]{0.9f, 0.2f, 0.3f, 0.6f, 0.1f, 0.4f, 0.2f, 0.9f, 0.5f});
        product.addTiePointGrid(t2);
        product.setGeoCoding(new TiePointGeoCoding(t1, t2, Datum.WGS_84));
    }

    public void testEnsureThatSubsetInfoAllwaysNull() throws IOException {
        ProductSubsetDef subsetDef = new ProductSubsetDef();
        Product product2 = projectionBuilder.readProductNodes(product, subsetDef);
        assertEquals(product.getName(), product2.getName());
        assertNull(projectionBuilder.getSubsetDef());
        assertNotNull(subsetDef);
        projectionBuilder.setSubsetDef(subsetDef);
        assertNull(projectionBuilder.getSubsetDef());
    }

    public void testCopyPlacemarkGroups() throws IOException {
        final PinSymbol defaultPinSymbol = PinSymbol.createDefaultPinSymbol();
        final Pin pin = new Pin("P1", "", "", new PixelPos(1.5f, 1.5f), null, defaultPinSymbol);
        final Pin gcp = new Pin("G1", "", "", new PixelPos(2.5f, 2.5f), null, defaultPinSymbol);

        product.getPinGroup().add(pin);
        product.getGcpGroup().add(gcp);

        final Product product2 = projectionBuilder.readProductNodes(product, null);

        assertEquals(1, product2.getPinGroup().getNodeCount());
        assertEquals(1, product2.getGcpGroup().getNodeCount());
        final Pin pin2 = product2.getPinGroup().get(0);
        final Pin gcp2 = product2.getGcpGroup().get(0);

        assertEquals("P1", pin2.getName());
        assertEquals("G1", gcp2.getName());

        assertEquals(pin.getGeoPos(), pin2.getGeoPos());
        assertEquals(gcp.getGeoPos(), gcp2.getGeoPos());

        final GeoCoding geoCoding = product2.getGeoCoding();
        final PixelPos pinPixelPos = geoCoding.getPixelPos(pin2.getGeoPos(), null);
        final PixelPos gcpPixelPos = geoCoding.getPixelPos(gcp2.getGeoPos(), null);

        assertEquals(pinPixelPos, pin2.getPixelPos());
        assertEquals(gcpPixelPos, gcp2.getPixelPos());
    }
}
