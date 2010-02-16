package com.bc.ceres.glayer.support.filters;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.core.Assert;

public class NotFilter implements LayerFilter {
    private final LayerFilter arg;

    public NotFilter(LayerFilter arg) {
        Assert.notNull(arg, "arg");
        this.arg = arg;
    }

    public LayerFilter getArg() {
        return arg;
    }

    public boolean accept(Layer layer) {
        return !arg.accept(layer);
    }
}