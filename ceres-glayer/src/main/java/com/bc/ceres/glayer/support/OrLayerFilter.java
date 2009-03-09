package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.core.Assert;

public class OrLayerFilter implements LayerFilter {
    private final LayerFilter filter1;
    private final LayerFilter filter2;

    public OrLayerFilter(LayerFilter filter1, LayerFilter filter2) {
        this.filter1 = filter1;
        this.filter2 = filter2;
    }

    public LayerFilter getFilter1() {
        return filter1;
    }

    public LayerFilter getFilter2() {
        return filter2;
    }

    public boolean accept(Layer layer) {
        return filter1.accept(layer) || filter2.accept(layer);
    }
}