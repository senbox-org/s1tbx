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
import static junit.framework.Assert.assertSame;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.draw.LineFigure;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Collections;

public class FigureLayerTypeTest extends LayerTypeTest {

    public FigureLayerTypeTest() {
        super(FigureLayerType.class);
    }

    @Test
    public void testConfigurationTemplate() {
        final PropertySet template = getLayerType().createLayerConfig(null);

        assertNotNull(template);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTLINED, Boolean.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTL_COLOR, Color.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTL_TRANSPARENCY, Double.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_OUTL_WIDTH, Double.class);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_FILLED, Boolean.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_FILL_COLOR, Color.class);
        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_SHAPE_FILL_TRANSPARENCY, Double.class);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_TRANSFORM, AffineTransform.class);

        ensurePropertyIsDefined(template, FigureLayer.PROPERTY_NAME_FIGURE_LIST, ArrayList.class);

    }

    @Test
    public void testCreateLayer() {
        final Product product = new Product("N", "T", 10, 10);
        final Band raster = new VirtualBand("A", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(raster);

        final PropertySet config = getLayerType().createLayerConfig(null);
        final ArrayList figureList = new ArrayList();
        figureList.add(new LineFigure(new Rectangle(0, 0, 10, 10), Collections.EMPTY_MAP));
        config.setValue(FigureLayer.PROPERTY_NAME_FIGURE_LIST, figureList);
        config.setValue(FigureLayer.PROPERTY_NAME_TRANSFORM, new AffineTransform());

        final Layer layer = getLayerType().createLayer(null, config);
        assertNotNull(layer);
        assertSame(getLayerType(), layer.getLayerType());
        assertTrue(layer instanceof FigureLayer);
    }
}