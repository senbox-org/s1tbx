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


import org.esa.snap.core.util.ProductUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;

import static org.junit.Assert.*;

public class Scene_MultiSize_Test {

    private Product srcProduct;
    private Product dstProduct;
    private Band sourceB1;
    private Band sourceB2;
    private Band sourceB3;
    private Band sourceB4;
    private Band destB1;
    private Band destB2;
    private Band destB3;
    private Band destB4;

    @Before
    public void setUp() throws Exception {
        srcProduct = new Product("srcProduct", "pType", 100, 200);

        CrsGeoCoding sceneGC = createCrsGeoCoding(srcProduct.getSceneRasterSize(), 1 * 0.1);
        srcProduct.setSceneGeoCoding(sceneGC);
        sourceB1 = new Band("B1", ProductData.TYPE_INT8, 30, 60);
        sourceB1.setGeoCoding(createCrsGeoCoding(sourceB1.getRasterSize(), (100f / 30f) * 0.1));
        srcProduct.addBand(sourceB1);
        sourceB2 = new Band("B2", ProductData.TYPE_INT8, 30, 60);
        sourceB2.setGeoCoding(createCrsGeoCoding(sourceB2.getRasterSize(), (100f / 30f) * 0.1));
        srcProduct.addBand(sourceB2);
        sourceB3 = new Band("B3", ProductData.TYPE_INT8, 15, 30);
        sourceB3.setGeoCoding(createCrsGeoCoding(sourceB3.getRasterSize(), (100f / 15f) * 0.1));
        srcProduct.addBand(sourceB3);
        sourceB4 = new Band("B4", ProductData.TYPE_INT8, 100, 200);
        sourceB4.setGeoCoding(createCrsGeoCoding(sourceB4.getRasterSize(), 1 * 0.1));
        srcProduct.addBand(sourceB4);

        dstProduct = new Product("destProduct", "pType", 100, 200);
        destB1 = ProductUtils.copyBand(sourceB1.getName(), srcProduct, dstProduct, true);
        destB2 = ProductUtils.copyBand(sourceB2.getName(), srcProduct, dstProduct, true);
        destB3 = ProductUtils.copyBand(sourceB3.getName(), srcProduct, dstProduct, true);
        destB4 = ProductUtils.copyBand(sourceB4.getName(), srcProduct, dstProduct, true);

    }


    @Test
    public void testTransferMultiSizeGCToProduct() throws Exception {

        final Scene srcScene = SceneFactory.createScene(srcProduct);
        assertNotNull(srcScene);
        final Scene destScene = SceneFactory.createScene(dstProduct);
        assertNotNull(destScene);

        assertTrue(srcScene.transferGeoCodingTo(destScene, null));

        assertNotNull(dstProduct.getSceneGeoCoding());
        assertFalse(dstProduct.isUsingSingleGeoCoding());
        assertNotNull(destB1.getGeoCoding());
        assertNotNull(destB2.getGeoCoding());
        assertNotNull(destB3.getGeoCoding());
        assertNotNull(destB4.getGeoCoding());

        compareLastGeoPos(srcProduct, dstProduct);
        compareLastGeoPos(sourceB1, destB1);
        compareLastGeoPos(sourceB2, destB2);
        compareLastGeoPos(sourceB3, destB3);
        compareLastGeoPos(sourceB4, destB4);

        Rectangle srcBounds = ProductUtils.createGeoBoundaryPaths(srcProduct)[0].getBounds();
        Rectangle dstBounds = ProductUtils.createGeoBoundaryPaths(dstProduct)[0].getBounds();
        assertEquals(srcBounds, dstBounds);

    }

    private void compareLastGeoPos(Product sourceProduct, Product destProduct) {
        PixelPos srcPP = new PixelPos(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        GeoPos srcGP = sourceProduct.getSceneGeoCoding().getGeoPos(srcPP, null);
        PixelPos dstPP = new PixelPos(destProduct.getSceneRasterWidth(), destProduct.getSceneRasterHeight());
        GeoPos dstGP = destProduct.getSceneGeoCoding().getGeoPos(dstPP, null);
        assertEquals(srcGP, dstGP);
    }

    private void compareLastGeoPos(Band sourceBand, Band destBand) {
        PixelPos srcPP = new PixelPos(sourceBand.getRasterWidth(), sourceBand.getRasterHeight());
        GeoPos srcGP = sourceBand.getGeoCoding().getGeoPos(srcPP, null);
        PixelPos dstPP = new PixelPos(destBand.getRasterWidth(), destBand.getRasterHeight());
        GeoPos dstGP = destBand.getGeoCoding().getGeoPos(dstPP, null);
        assertEquals(srcGP, dstGP);
    }


    private CrsGeoCoding createCrsGeoCoding(Dimension imageDimension, double scale) throws Exception {
        AffineTransform i2m = new AffineTransform();
        final int northing = 60;
        final int easting = 5;
        i2m.translate(easting, northing);
        i2m.scale(scale, -scale);
        return new CrsGeoCoding(DefaultGeographicCRS.WGS84, new Rectangle(imageDimension), i2m);
    }


}
