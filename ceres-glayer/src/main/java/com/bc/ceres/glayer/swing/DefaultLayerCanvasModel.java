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

package com.bc.ceres.glayer.swing;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.grender.support.DefaultViewport;

public class DefaultLayerCanvasModel implements LayerCanvasModel {

    private final Layer layer;
    private final Viewport viewport;

    public DefaultLayerCanvasModel() {
        this(new CollectionLayer(), new DefaultViewport());
    }

    public DefaultLayerCanvasModel(Layer layer, Viewport viewport) {
        Assert.notNull(layer, "layer");
        Assert.notNull(viewport, "viewport");
        this.layer = layer;
        this.viewport = viewport;
    }

    public Layer getLayer() {
        return layer;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public void addChangeListener(ChangeListener listener) {
        getLayer().addListener(listener);
        getViewport().addListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        getLayer().removeListener(listener);
        getViewport().removeListener(listener);
    }
}
