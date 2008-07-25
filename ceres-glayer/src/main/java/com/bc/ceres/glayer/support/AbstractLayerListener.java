package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.CollectionLayer;

import java.beans.PropertyChangeEvent;
import java.awt.geom.Rectangle2D;

/**
 * An (empty) {@code LayerListener} implementation.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class AbstractLayerListener implements LayerListener {
    /**
     * Called if a property of the given layer has changed. The source of the property change event
     * may be either the layer itself or its style (see {@link Layer#getStyle()}).
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
     * @param layer The layer which triggered the change.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    @Override
    public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
    }

    /**
     * Called if a new layer has been added to a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param layers      The layers added.
     */
    @Override
    public void handleLayersAdded(CollectionLayer parentLayer, Layer[] layers) {
    }

    /**
     * Called if an existing layer has been removed from a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param layers      The layers removed.
     */
    @Override
    public void handleLayersRemoved(CollectionLayer parentLayer, Layer[] layers) {
    }
}
