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

import com.bc.ceres.glevel.MultiLevelImage;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class VectorDataMultiLevelImageTest {

    private Product product;
    private VectorDataNode vectorDataNode;
    private VectorDataMultiLevelImage image1;
    private VectorDataMultiLevelImage image2;

    @Before
    public void setup() {

        product = new Product("P", "T", 8, 8);

        vectorDataNode = new VectorDataNode("vectorDataNode", Placemark.createGeometryFeatureType());

        product.getVectorDataGroup().add(vectorDataNode);
        Band b1 = new VirtualBand("B1", ProductData.TYPE_FLOAT64, 8, 8, "1*X + 1*Y");
        b1.setImageToModelTransform(new AffineTransform());
        product.addBand(b1);

        Band b2 = new VirtualBand("B2", ProductData.TYPE_FLOAT64, 4, 4, "2*X + 2*Y");
        b2.setImageToModelTransform(AffineTransform.getScaleInstance(2.0, 2.0));
        product.addBand(b2);

        image1 = VectorDataMultiLevelImage.createMaskImage(vectorDataNode, product.getBand("B1"));
        image2 = VectorDataMultiLevelImage.createMaskImage(vectorDataNode, product.getBand("B2"));
    }

    @Test
    public void imageIsUpdated() {

        int[] expectedMask1 = {
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 1, 1, 1, 1, 0, 0,
                0, 0, 1, 1, 1, 1, 0, 0,
                0, 0, 1, 1, 1, 1, 0, 0,
                0, 0, 1, 1, 1, 1, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0,
        };

        int[] expectedMask2 = {
                0, 0, 0, 0,
                0, 1, 1, 0,
                0, 1, 1, 0,
                0, 0, 0, 0,
        };

        assertSameMaskData(new int[8 * 8], this.image1);
        assertSameMaskData(new int[4 * 4], this.image2);

        vectorDataNode.getFeatureCollection().add(
                createFeature("rectangle", new Rectangle2D.Double(2.5, 2.5, 4.0, 4.0)));

        assertSameMaskData(expectedMask1, this.image1);
        assertSameMaskData(expectedMask2, this.image2);
    }

    private void assertSameMaskData(int[] expectedMask, MultiLevelImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        assertEquals(expectedMask.length, w * h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int sample = 255 * expectedMask[y * w + x];
                assertEquals(String.format("x=%d, y=%d", x, y), sample, image.getData().getSample(x, y, 0));
            }
        }
    }

    @Test
    public void listenerIsAdded() {
        assertTrue(Arrays.asList(product.getProductNodeListeners()).contains(image1));
        assertTrue(Arrays.asList(product.getProductNodeListeners()).contains(image2));
    }

    @Test
    public void listenerIsRemoved() {
        image1.dispose();
        image2.dispose();
        assertFalse(Arrays.asList(product.getProductNodeListeners()).contains(image1));
        assertFalse(Arrays.asList(product.getProductNodeListeners()).contains(image2));
    }

    @Test
    public void vectorDataIsSet() {
        assertSame(vectorDataNode, image1.getVectorDataNode());
        assertSame(vectorDataNode, image2.getVectorDataNode());
    }

    @Test
    public void vectorDataIsClearedWhenImagesIsDisposed() {
        image1.dispose();
        image2.dispose();
        assertEquals(null, image1.getVectorDataNode());
        assertEquals(null, image2.getVectorDataNode());
    }

    private static SimpleFeature createFeature(String name, Rectangle2D rectangle) {
        final SimpleFeatureType type = Placemark.createGeometryFeatureType();
        final SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);
        final GeometryFactory geometryFactory = new GeometryFactory();
        final double x = rectangle.getX();
        final double y = rectangle.getY();
        final double w = rectangle.getWidth();
        final double h = rectangle.getHeight();
        final LinearRing linearRing = geometryFactory.createLinearRing(new Coordinate[]{
                new Coordinate(x, y),
                new Coordinate(x + w, y),
                new Coordinate(x + w, y + h),
                new Coordinate(x, y + h),
                new Coordinate(x, y),
        });
        featureBuilder.add(geometryFactory.createPolygon(linearRing, null));
        return featureBuilder.buildFeature(name);
    }
}
