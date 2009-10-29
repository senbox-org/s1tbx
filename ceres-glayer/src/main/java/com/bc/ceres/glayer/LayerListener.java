package com.bc.ceres.glayer;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

/**
 * A change listener.
 *
 * @author Norman Fomferra
 */
public interface LayerListener {
    /**
     * Called if a property of the given layer has changed. The source of the property change event
     * may be either the layer itself or its style (see {@link Layer#getStyle()}).
     *
     * @param layer The layer which triggered the change.
     * @param event The layer property change event.
     */
    void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event);

    /**
     * Called if the data of the given layer has changed.
     *
     * @param layer       The layer which triggered the change.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion);

    /**
     * Called if a new layer has been added to a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param childLayers The child layers added.
     */
    void handleLayersAdded(Layer parentLayer, Layer[] childLayers);

    /**
     * Called if an existing layer has been removed from a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param childLayers The child layers removed.
     */
    void handleLayersRemoved(Layer parentLayer, Layer[] childLayers);
}
