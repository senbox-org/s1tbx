package com.bc.ceres.glayer.support.filters;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.core.Assert;

public class NameFilter implements LayerFilter {
    private final String name;

    public NameFilter(String name) {
        Assert.notNull(name, "name");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean accept(Layer layer) {
        return name.equalsIgnoreCase(layer.getName());
    }
}
