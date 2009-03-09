package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;

import java.util.List;

public class LayerUtils {
    
    public static int getLayerIndex(Layer rootLayer, LayerFilter filter, int defaultIndex) {
        List<Layer> children = rootLayer.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Layer child = children.get(i);
            if (findAcceptedLayer(child, filter) != null) {
                return i;
            }
        }
        return defaultIndex;
    }

    private static Layer findAcceptedLayer(Layer layer, LayerFilter filter) {
        if (filter.accept(layer)) {
            return layer;
        }
        for (Layer child : layer.getChildren()) {
            Layer acceptedLayer = findAcceptedLayer(child, filter);
            if (acceptedLayer != null) {
                return acceptedLayer;
            }
        }
        return null;
    }

    private LayerUtils() {
    }

}
