package com.bc.ceres.glayer;

import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Iterator;

public abstract class LayerType extends ExtensibleObject {
    private static ServiceRegistry<LayerType> registry = ServiceRegistryFactory.getInstance().getServiceRegistry(LayerType.class);

    protected LayerType() {
    }

    public abstract String getName();

    public abstract boolean isValidFor(LayerContext ctx);

    public abstract Layer createLayer(LayerContext ctx, Map<String, Object> configuration);

    public abstract Map<String, Object> createConfiguration(LayerContext ctx, Layer layer);


    public static LayerType getLayerType(String layerTypeClassName) {
          return registry.getService(layerTypeClassName);
    }

    static {

        ServiceLoader sl = ServiceLoader.load(LayerType.class);
        Iterator it = sl.iterator();
        while (it.hasNext()) {
            registry.addService((LayerType) it.next());
        }


    }

}
