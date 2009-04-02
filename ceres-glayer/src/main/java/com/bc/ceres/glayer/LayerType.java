package com.bc.ceres.glayer;

import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;

import java.util.Map;
import java.util.ServiceLoader;

public abstract class LayerType extends ExtensibleObject {
    private static final ServiceRegistry<LayerType> REGISTRY;

    protected LayerType() {
    }

    public abstract String getName();

    public abstract boolean isValidFor(LayerContext ctx);

    public abstract Layer createLayer(LayerContext ctx, Map<String, Object> configuration);

    public abstract Map<String, Object> createConfiguration(LayerContext ctx, Layer layer);


    public static LayerType getLayerType(String layerTypeClassName) {
          return REGISTRY.getService(layerTypeClassName);
    }

    static {
        REGISTRY = ServiceRegistryFactory.getInstance().getServiceRegistry(LayerType.class);

        final ServiceLoader<LayerType> serviceLoader = ServiceLoader.load(LayerType.class);
        for (final LayerType layerType : serviceLoader) {
            REGISTRY.addService(layerType);
        }
    }
}
