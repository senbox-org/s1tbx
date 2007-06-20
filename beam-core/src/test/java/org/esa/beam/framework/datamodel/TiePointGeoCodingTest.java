/*
 * $Id: TiePointGeoCodingTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;

public class TiePointGeoCodingTest extends TestCase {

    public void testThatLonGridIsNormalized() {
        final PixelPos pixelPos = new PixelPos();
        TiePointGeoCoding gc = createTestGeoCoding();
        assertTrue(gc.isCrossingMeridianAt180());
        gc.getPixelPos(new GeoPos(31.25f, -177.5f), pixelPos);
        assertEquals(4.5f, pixelPos.y, 1e-5f);
        assertEquals(7.5f, pixelPos.x, 1e-5f);
    }

    public void testThatReturnedPositionsNotNull() {
        TiePointGeoCoding gc = createTestGeoCoding();
        assertNotNull(gc.getPixelPos(new GeoPos(31.25f, -177.5f), null));
        assertNotNull(gc.getGeoPos(new PixelPos(2,2), null));
    }

    public void testTransferGeoCodingWithoutSubset() {
        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 300, 400);
        final Band destNode = new Band("destDummy", ProductData.TYPE_INT8, 50, 50);


        final Product srcProduct = createTestProduct("T1", 300, 400);
        srcProduct.addBand(srcNode);

        final Product destProduct = createTestProduct("T2", 50, 50);
        destProduct.addBand(destNode);

        final Scene destScene = SceneFactory.createScene(destProduct);
        final Scene srcScene = SceneFactory.createScene(srcProduct);

        srcScene.transferGeoCodingTo(destScene, null);

        final TiePointGeoCoding copyGeoCoding = (TiePointGeoCoding) destScene.getGeoCoding();

        TiePointGeoCoding gc = createTestGeoCoding();
        assertNotSame(gc, copyGeoCoding);
        assertNotSame(gc.getLatGrid(), copyGeoCoding.getLatGrid());
        assertNotSame(gc.getLonGrid(), copyGeoCoding.getLonGrid());
        assertEquals(gc.getLatGrid().getName(), copyGeoCoding.getLatGrid().getName());
        assertEquals(gc.getLonGrid().getName(), copyGeoCoding.getLonGrid().getName());
    }


    public void testTransferGeoCodingWithtSubset() {
        final Band srcNode = new Band("srcDummy", ProductData.TYPE_INT8, 300, 400);
        final Band destNode = new Band("destDummy", ProductData.TYPE_INT8, 50, 50);
        final ProductSubsetDef subsetDef = new ProductSubsetDef("subset");
        subsetDef.setRegion(10, 10, 50, 50);
        subsetDef.setSubSampling(2, 3);


        final Product srcProduct = createTestProduct("T1", 300, 400);
        srcProduct.addBand(srcNode);

        final Product destProduct = createTestProduct("T2", 50, 50);
        destProduct.addBand(destNode);

        final Scene destScene = SceneFactory.createScene(destProduct);
        final Scene srcScene = SceneFactory.createScene(srcProduct);

        srcScene.transferGeoCodingTo(destScene, subsetDef);

        final TiePointGeoCoding copyGeoCoding = (TiePointGeoCoding) destScene.getGeoCoding();

        TiePointGeoCoding gc = createTestGeoCoding();
        assertNotSame(gc, copyGeoCoding);
        assertNotSame(gc.getLatGrid(), copyGeoCoding.getLatGrid());
        assertNotSame(gc.getLonGrid(), copyGeoCoding.getLonGrid());
        assertEquals(gc.getLatGrid().getName(), copyGeoCoding.getLatGrid().getName());
        assertEquals(gc.getLonGrid().getName(), copyGeoCoding.getLonGrid().getName());

    }

    private Product createTestProduct(String name, int w, int h) {
        TiePointGeoCoding gc = createTestGeoCoding();
        final Product srcProduct = new Product(name, "t", w, h);
        srcProduct.addTiePointGrid(gc.getLonGrid());
        srcProduct.addTiePointGrid(gc.getLatGrid());
        srcProduct.setGeoCoding(gc);
        return srcProduct;
    }

    private TiePointGeoCoding createTestGeoCoding() {
        TiePointGrid latGrid = new TiePointGrid("lat", 3, 4, 0.5f, 0.5f, 4, 4,
                                                new float[]{
                                                        43.0f, 42.0f, 41.0f,
                                                        33.0f, 32.0f, 31.0f,
                                                        23.0f, 22.0f, 21.0f,
                                                        13.0f, 12.0f, 11.0f
                                                });
        TiePointGrid lonGrid = new TiePointGrid("lon", 3, 4, 0.5f, 0.5f, 4, 4,
                                                new float[]{
                                                        176.5f, -179.5f, -175.5f,
                                                        175.5f, 179.5f, -176.5f,
                                                        174.5f, 178.5f, -177.5f,
                                                        173.5f, 177.5f, -178.5f
                                                },
                                                TiePointGrid.DISCONT_AT_180);
        return new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
    }
}
