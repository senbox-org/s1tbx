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

package com.bc.ceres.glayer.support;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.annotations.LayerTypeMetadata;
import com.bc.ceres.grender.Rendering;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;

/**
 * A background layer is used to draw a background using a unique {@link java.awt.Color}.
 *
 * @author Norman Fomferra
 */
public class BackgroundLayer extends Layer {

    private static final Type LAYER_TYPE = LayerTypeRegistry.getLayerType(Type.class);

    public BackgroundLayer(Color color) {
        this(LAYER_TYPE, initConfiguration(LAYER_TYPE.createLayerConfig(null), color));
    }

    public BackgroundLayer(Type type, PropertySet configuration) {
        super(type, configuration);
    }

    private static PropertySet initConfiguration(PropertySet configuration, Color color) {
        configuration.setValue(Type.COLOR, color);
        return configuration;
    }

    Color getColor() {
        return (Color) getConfiguration().getValue(Type.COLOR);
    }

    void setColor(Color color) {
        getConfiguration().setValue(Type.COLOR, color);
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        final Graphics2D g = rendering.getGraphics();
        Paint oldPaint = g.getPaint();
        g.setPaint(getColor());
        Rectangle bounds = g.getClipBounds();
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setPaint(oldPaint);
    }

    @LayerTypeMetadata(name = "BackgroundLayerType",
                       aliasNames = {"com.bc.ceres.glayer.support.BackgroundLayer$Type"})
    public static class Type extends LayerType {

        private static final String COLOR = "color";

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            final PropertyContainer template = new PropertyContainer();
            template.addProperty(Property.create(COLOR, Color.class));

            return template;

        }

        @Override
        public Layer createLayer(LayerContext ctx, PropertySet configuration) {
            final BackgroundLayer layer = new BackgroundLayer(this, configuration);
            layer.setName("Background Layer");
            return layer;
        }
    }
}