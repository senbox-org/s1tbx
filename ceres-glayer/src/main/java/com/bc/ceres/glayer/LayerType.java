package com.bc.ceres.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.Assert;

import java.util.ServiceLoader;

public abstract class LayerType extends ExtensibleObject {

    private static final ServiceRegistry<LayerType> REGISTRY;

    protected LayerType() {
    }

    public abstract String getName();

    public abstract boolean isValidFor(LayerContext ctx);

    public final Layer createLayer(LayerContext ctx, ValueContainer configuration) {
        for (final ValueModel expectedModel : getConfigurationTemplate().getModels()) {
            final String propertyName = expectedModel.getDescriptor().getName();
            final ValueModel actualModel = configuration.getModel(propertyName);
            if (actualModel != null) {
                try {
                    expectedModel.validate(actualModel.getValue());
                } catch (ValidationException e) {
                    throw new IllegalArgumentException(String.format(
                            "Invalid value for property '%s': %s", propertyName, e.getMessage()), e);
                }
            } else {
                throw new IllegalArgumentException(String.format(
                        "No model defined for property '%s'", propertyName));
            }
        }

        return createLayerImpl(ctx, configuration);
    }

    protected abstract Layer createLayerImpl(LayerContext ctx, ValueContainer configuration);

    public abstract ValueContainer getConfigurationTemplate();

    public ValueContainer getConfigurationCopy(LayerContext ctx, Layer layer) {
        final ValueContainer configuration = new ValueContainer();

        for (ValueModel model : layer.getConfiguration().getModels()) {
            final String name = model.getDescriptor().getName();
            final Class<?> type = model.getDescriptor().getType();
            final ValueDescriptor descriptor = new ValueDescriptor(name, type);
            final DefaultValueAccessor valueAccessor = new DefaultValueAccessor();
            valueAccessor.setValue(model.getValue());
            configuration.addModel(new ValueModel(descriptor, valueAccessor));
        }

        return configuration;
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
