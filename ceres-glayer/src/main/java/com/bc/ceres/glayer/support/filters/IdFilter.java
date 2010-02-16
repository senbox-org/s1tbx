package com.bc.ceres.glayer.support.filters;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.core.Assert;

public class IdFilter implements LayerFilter {
    private final String id;

    public IdFilter(String id) {
        Assert.notNull(id, "id");
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean accept(Layer layer) {
        return id.equals(layer.getId());
    }
}