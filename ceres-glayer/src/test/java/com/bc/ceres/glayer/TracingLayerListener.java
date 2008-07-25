package com.bc.ceres.glayer;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

public class TracingLayerListener implements LayerListener {

    public String trace = "";

    public void propertyChange(PropertyChangeEvent event) {
        trace += event.getPropertyName() + ";";
    }

    public void handleLayerStylePropertyChanged(Layer layer, PropertyChangeEvent event) {
        trace += "style." + event.getPropertyName() + ";";
    }

    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
        trace += event.getPropertyName() + ";";
    }

    public void handleLayerDataChanged(Layer layer, Rectangle2D region) {
        trace += "data " + region + ";";
    }

    public void handleLayersAdded(CollectionLayer parentLayer, Layer[] layers) {
        trace += "added " + layers.length + ";";
    }

    public void handleLayersRemoved(CollectionLayer parentLayer, Layer[] layers) {
        trace += "removed " + layers.length + ";";
    }
}
