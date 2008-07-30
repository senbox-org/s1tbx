package com.bc.ceres.glayer;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

public class TracingLayerListener implements LayerListener {

    public String trace = "";

    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
        trace += event.getPropertyName() + ";";
    }

    public void handleLayerDataChanged(Layer layer, Rectangle2D region) {
        trace += "data " + region + ";";
    }

    public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
        trace += "added " + childLayers.length + ";";
    }

    public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
        trace += "removed " + childLayers.length + ";";
    }
}
