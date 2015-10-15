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

package org.esa.snap.core.layer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class RasterImageLayerTypeTest extends LayerTypeTest {

    public RasterImageLayerTypeTest() {
        super(RasterImageLayerType.class);
    }

    @Test
    public void testDefaultConfiguration() {
        final LayerType layerType = getLayerType();

        final PropertySet template = layerType.createLayerConfig(null);
        assertNotNull(template);

        ensurePropertyIsDeclaredButNotDefined(template, "raster", RasterDataNode.class);
        ensurePropertyIsDefined(template, "borderShown", Boolean.class);
        ensurePropertyIsDefined(template, "borderWidth", Double.class);
        ensurePropertyIsDefined(template, "borderColor", Color.class);
    }

    @Test
    public void testCreateLayerWithSingleRaster() {
        final RasterImageLayerType layerType = (RasterImageLayerType) getLayerType();

        final Product product = new Product("N", "T", 10, 10);
        final Band raster = new VirtualBand("A", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(raster);

        final ImageLayer imageLayer = (ImageLayer) layerType.createLayer(raster, null);
        assertNotNull(imageLayer.getMultiLevelSource());
    }

}
