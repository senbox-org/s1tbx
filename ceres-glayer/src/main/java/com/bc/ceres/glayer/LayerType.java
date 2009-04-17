package com.bc.ceres.glayer;

import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;

import java.util.ServiceLoader;

public abstract class LayerType extends ExtensibleObject {
    private static final ServiceRegistry<LayerType> REGISTRY;

    protected LayerType() {
    }

    public abstract String getName();

    public abstract boolean isValidFor(LayerContext ctx);

    public abstract Layer createLayer(LayerContext ctx, ValueContainer configuration);

    public abstract ValueContainer getConfigurationCopy(LayerContext ctx, Layer layer);

    public ValueContainer createConfigurationTemplate() {
        return new ValueContainer();   
    }

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

    protected static ValueModel createDefaultValueModel(String propertyName, Object value) {
        final ValueDescriptor descriptor = new ValueDescriptor(propertyName, value.getClass());
        final DefaultValueAccessor accessor = new DefaultValueAccessor();
        accessor.setValue(value);
        
        return new ValueModel(descriptor, accessor);
    }

    protected static ValueModel createDefaultValueModel(String propertyName, Class<?> type) {
        final ValueDescriptor descriptor = new ValueDescriptor(propertyName, type);
        final DefaultValueAccessor accessor = new DefaultValueAccessor();

        return new ValueModel(descriptor, accessor);
    }
}
