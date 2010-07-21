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

package org.esa.beam.glayer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class PlacemarkLayerTest extends TestCase {

    public void testConstruction() {
        final Product product = new Product("N", "T", 16, 16);
        final PinDescriptor pmd = PinDescriptor.INSTANCE;
        final AffineTransform i2m = new AffineTransform();

        final LayerType type = LayerTypeRegistry.getLayerType(PlacemarkLayerType.class);
        final PropertySet template = type.createLayerConfig(null);
        template.setValue("product", product);
        template.setValue("placemarkDescriptor", pmd);
        template.setValue("imageToModelTransform", i2m);
        final PlacemarkLayer placemarkLayer = (PlacemarkLayer) type.createLayer(null, template);
        assertEquals(product, placemarkLayer.getProduct());
        assertEquals(pmd, placemarkLayer.getPlacemarkDescriptor());
        assertEquals(i2m, placemarkLayer.getImageToModelTransform());

        assertTrue(type instanceof PlacemarkLayerType);
    }

    public void testLayerDataChanges() {
        final Product product = new Product("N", "T", 16, 16);
        final PinDescriptor pmd = PinDescriptor.INSTANCE;
        final AffineTransform i2m = new AffineTransform();

        final PlacemarkLayer placemarkLayer1 = new PlacemarkLayer(product, pmd, i2m);
        final PlacemarkLayer placemarkLayer2 = new PlacemarkLayer(product, pmd, i2m);

        placemarkLayer1.setName("L1");
        placemarkLayer2.setName("L2");

        final MyLayerListener layerListener = new MyLayerListener();
        placemarkLayer1.addListener(layerListener);
        placemarkLayer2.addListener(layerListener);
        assertEquals("", layerListener.trace);

        product.getPinGroup().add(createPlacemark("P1"));
        assertEquals("L1;L2;", layerListener.trace);

        product.getPinGroup().add(createPlacemark("P2"));
        assertEquals("L1;L2;L1;L2;", layerListener.trace);

        placemarkLayer1.dispose();
        layerListener.trace = "";

        product.getPinGroup().add(createPlacemark("P3"));
        assertEquals("L2;", layerListener.trace);

        placemarkLayer2.dispose();
        layerListener.trace = "";

        product.getPinGroup().add(createPlacemark("P4"));
        assertEquals("", layerListener.trace);
    }

    private Placemark createPlacemark(String s) {
        return new Placemark(s, "L", "D", new PixelPos(), new GeoPos(), PinDescriptor.INSTANCE, null);
    }

    private static class MyLayerListener extends AbstractLayerListener {

        String trace = "";

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            trace += layer.getName() + ";";
        }
    }
}
