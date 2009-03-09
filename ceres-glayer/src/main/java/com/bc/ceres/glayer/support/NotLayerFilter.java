package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.core.Assert;

public class NotLayerFilter implements LayerFilter {
    private final LayerFilter filter;

    public NotLayerFilter(LayerFilter filter) {
        this.filter = filter;
    }

    public LayerFilter getFilter() {
        return filter;
    }

    public boolean accept(Layer layer) {
        return !filter.accept(layer);
    }
}