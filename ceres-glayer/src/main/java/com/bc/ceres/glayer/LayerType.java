package com.bc.ceres.glayer;

import com.bc.ceres.core.ExtensibleObject;

import java.util.Map;

public abstract class LayerType extends ExtensibleObject {

    protected LayerType() {
    }

    public abstract String getName();

    public abstract boolean isValidFor(LayerContext ctx);

    public abstract Layer createLayer(LayerContext ctx, Map<String, Object> configuration);

    public abstract Map<String, Object> createConfiguration(LayerContext ctx, Layer layer);
}
