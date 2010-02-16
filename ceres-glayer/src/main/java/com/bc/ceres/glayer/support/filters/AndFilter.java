package com.bc.ceres.glayer.support.filters;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.core.Assert;

public class AndFilter implements LayerFilter {
    private final LayerFilter arg1;
    private final LayerFilter arg2;

    public AndFilter(LayerFilter arg1, LayerFilter arg2) {
        Assert.notNull(arg1, "arg1");
        Assert.notNull(arg2, "arg2");
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public LayerFilter getArg1() {
        return arg1;
    }

    public LayerFilter getArg2() {
        return arg2;
    }

    public boolean accept(Layer layer) {
        return arg1.accept(layer) && arg2.accept(layer);
    }
}