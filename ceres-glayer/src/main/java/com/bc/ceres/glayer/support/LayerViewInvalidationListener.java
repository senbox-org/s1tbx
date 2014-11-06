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

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

/**
 * An {@code LayerListener} implementation which delegates a visible layer changes to the abstract
 * {@link #handleViewInvalidation(com.bc.ceres.glayer.Layer, java.awt.geom.Rectangle2D)} handler.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class LayerViewInvalidationListener extends AbstractLayerListener {
    /**
     * Called if a property of the given layer has changed.
     * Calls {@link #handleViewInvalidation(com.bc.ceres.glayer.Layer, java.awt.geom.Rectangle2D)} only if the change event is visible.
     *
     * @param layer               The layer.
     * @param propertyChangeEvent The layer property change event. May be null, if the entire style changed.
     */
    @Override
    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent propertyChangeEvent) {
        if (isVisibleChangeEvent(propertyChangeEvent)) {
            handleViewInvalidation(layer, null);
        }
    }

    /**
     * Called if the data of the given layer has changed.
     * Calls {@link #handleViewInvalidation(com.bc.ceres.glayer.Layer, java.awt.geom.Rectangle2D)}.
     *
     * @param layer       The layer.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    @Override
    public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
        handleViewInvalidation(layer, modelRegion);
    }

    /**
     * Called if a new layer has been added to a collection layer.
     * Calls {@link #handleViewInvalidation(com.bc.ceres.glayer.Layer, java.awt.geom.Rectangle2D)}.
     *
     * @param parentLayer The parent layer.
     * @param childLayers The child layers added.
     */
    @Override
    public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
        // todo - region = union of bounds of all layers
        handleViewInvalidation(parentLayer, null);
    }

    /**
     * Called if an existing layer has been removed from a collection layer.
     * Calls {@link #handleViewInvalidation(com.bc.ceres.glayer.Layer, java.awt.geom.Rectangle2D)}.
     *
     * @param parentLayer The parent layer.
     * @param childLayers The child layers removed.
     */
    @Override
    public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
        // todo - region = union of bounds of all layers
        handleViewInvalidation(parentLayer, null);
    }

    /**
     * Called if a visible layer change occurred.
     *
     * @param layer       The layer.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    protected abstract void handleViewInvalidation(Layer layer, Rectangle2D modelRegion);
}