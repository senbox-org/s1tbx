package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Style;

import java.beans.PropertyChangeEvent;
import java.awt.geom.Rectangle2D;

/**
 * An {@code LayerListener} implementation which delegates a visible layer changes to the abstract
 * {@link #handleViewInvalidation(com.bc.ceres.glayer.Layer, java.awt.geom.Rectangle2D)} handler.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class LayerViewInvalidationListener implements LayerListener {
    /**
     * Called if a property of the given layer has changed.
     *
     * @param layer               The layer.
     * @param propertyChangeEvent The layer property change event. May be null, if the entire style changed.
     */
    @Override
    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent propertyChangeEvent) {
        if (propertyChangeEvent.getPropertyName().equals("visible")
                || propertyChangeEvent.getSource() instanceof Style) {
            handleViewInvalidation(layer, layer.getBounds());
        }
    }

    /**
     * Called if the data of the given layer has changed.
     *
     * @param layer  The layer.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    @Override
    public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
        handleViewInvalidation(layer, modelRegion);
    }

    /**
     * Called if a new layer has been added to a collection layer.
     *
     * @param parentLayer The parent layer.
     * @param layers      The layers added.
     */
    @Override
    public void handleLayersAdded(CollectionLayer parentLayer, Layer[] layers) {
        // todo - region = union of bounds of all layers
        handleViewInvalidation(parentLayer, null);
    }

    /**
     * Called if an existing layer has been removed from a collection layer.
     *
     * @param parentLayer The parent layer.
     * @param layers      The layers removed.
     */
    @Override
    public void handleLayersRemoved(CollectionLayer parentLayer, Layer[] layers) {
        // todo - region = union of bounds of all layers
        handleViewInvalidation(parentLayer, null);
    }

    /**
     * Called if a visible layer change occured.
     *
     * @param layer  The layer.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    public abstract void handleViewInvalidation(Layer layer, Rectangle2D modelRegion);
}