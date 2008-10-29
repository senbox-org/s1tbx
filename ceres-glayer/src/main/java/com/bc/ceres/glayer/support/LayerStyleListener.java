package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.Style;

import java.beans.PropertyChangeEvent;

/**
 * An {@code LayerListener} implementation which listens solely for layer style changes.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class LayerStyleListener extends AbstractLayerListener {
    /**
     * Delegates style changes to {@link com.bc.ceres.glayer.Layer#getStyle()}).
     *
     * @param layer The layer which triggered the change.
     * @param event The layer property change event.
     */
    @Override
    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
        if (event.getSource() instanceof Style) {
            handleLayerStylePropertyChanged(layer, event);
        }
    }

    /**
     * Called if a layer style change occured.
     *
     * @param layer The layer which triggered the change.
     * @param event The layer style property change event.
     */
    protected abstract void handleLayerStylePropertyChanged(Layer layer, PropertyChangeEvent event);
}