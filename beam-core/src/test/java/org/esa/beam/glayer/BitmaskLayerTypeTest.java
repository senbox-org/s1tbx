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
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import static junit.framework.Assert.assertSame;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.awt.Color;
import java.awt.geom.AffineTransform;

public class BitmaskLayerTypeTest extends LayerTypeTest {

    public BitmaskLayerTypeTest() {
        super(BitmaskLayerType.class);
    }

    @Test
    public void testConfigurationTemplate() {
        final PropertySet template = getLayerType().createLayerConfig(null);

        assertNotNull(template);
        ensurePropertyIsDeclaredButNotDefined(template, "bitmaskDef", BitmaskDef.class);
        ensurePropertyIsDeclaredButNotDefined(template, "product", Product.class);

        ensurePropertyIsDefined(template, "borderShown", Boolean.class);
        ensurePropertyIsDefined(template, "borderWidth", Double.class);
        ensurePropertyIsDefined(template, "borderColor", Color.class);
    }

    @Test
    public void testCreateLayer() {
        final Product product = new Product("N", "T", 10, 10);
        final Band raster = new VirtualBand("A", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(raster);
        final BitmaskDef bitmaskDef = new BitmaskDef("bitmask", "description", "A == 42", Color.BLUE, 0.4f);
        product.addBitmaskDef(bitmaskDef);

        final PropertySet config = getLayerType().createLayerConfig(null);
        config.setValue("product", product);
        config.setValue("bitmaskDef", bitmaskDef);

        final Layer layer = getLayerType().createLayer(null, config);
        assertNotNull(layer);
        
        MaskLayerType maskLayerType = LayerTypeRegistry.getLayerType(MaskLayerType.class);
        assertSame(maskLayerType, layer.getLayerType());
        assertTrue(layer instanceof ImageLayer);
        ImageLayer imageLayer = (ImageLayer) layer;
        assertNotNull(imageLayer.getMultiLevelSource());
    }
}
