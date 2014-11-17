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

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.binding.PropertyContainer;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

/**
 * An (empty) {@code LayerListener} implementation.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class AbstractLayerListener implements LayerListener {

    /**
     * Called if a property of the given layer has changed. The source of the property change event
     * may be either the layer itself or its configuration (see {@link Layer#getConfiguration()}).
     *
     * @param layer The layer which triggered the change.
     * @param event The layer property change event.
     */
    @Override
    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
    }

    /**
     * Called if the data of the given layer has changed.
     *
     * @param layer       The layer which triggered the change.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    @Override
    public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
    }

    /**
     * Called if a new layer has been added to a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param childLayers The child layers added.
     */
    @Override
    public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
    }

    /**
     * Called if an existing layer has been removed from a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param childLayers The child layers removed.
     */
    @Override
    public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
    }

    /**
     * @param propertyChangeEvent A property change event fired by a {@link Layer}.
     * @return {@code true} if the change event is visible, e.g. causes (re)rendering of some kind of view
     */
    protected boolean isVisibleChangeEvent(PropertyChangeEvent propertyChangeEvent) {
        return propertyChangeEvent.getPropertyName().equals("visible")
                || propertyChangeEvent.getPropertyName().equals("transparency")
                || propertyChangeEvent.getPropertyName().equals("swipePercent")
                || propertyChangeEvent.getPropertyName().equals("composite")
                || propertyChangeEvent.getSource() instanceof PropertyContainer;
    }
}
