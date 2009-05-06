package com.bc.ceres.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.accessors.DefaultValueAccessor;
import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;

import java.util.ServiceLoader;

// todo - Layer API: the API of this class is confusing and it is hard to implement subclasses. (nf)

// todo - Layer API: carefully javadoc it (nf)
public abstract class LayerType extends ExtensibleObject {

    private static final ServiceRegistry<LayerType> REGISTRY;

    protected LayerType() {
    }

    public abstract String getName();

    // todo - Layer API: it shall be safe for BEAM layers to cast ctx into a ProductSceneViewContext, otherwise there can be no reasonable implementations of this method beside CRS checkings (nf)
    public abstract boolean isValidFor(LayerContext ctx);

    // todo - Layer API: this seems to be the only framework usage of getConfigurationTemplate()! (nf)
    // todo - Layer API: why is this final? assume overriding it in order to cast ctx into an application-specific ctx (nf)
    public final Layer createLayer(LayerContext ctx, ValueContainer configuration) {
        for (final ValueModel expectedModel : getConfigurationTemplate().getModels()) {
            final String propertyName = expectedModel.getDescriptor().getName();
            final ValueModel actualModel = configuration.getModel(propertyName);
            if (actualModel != null) {
                try {
                    if (actualModel.getValue() == null && actualModel.getDescriptor().isNotNull()) {
                        actualModel.setValue(actualModel.getDescriptor().getDefaultValue());
                    }
                    expectedModel.validate(actualModel.getValue());
                } catch (ValidationException e) {
                    throw new IllegalArgumentException(String.format(
                            "Invalid value for property '%s': %s", propertyName, e.getMessage()), e);
                }
            } else {
                // todo - Layer API: why not copy from template if not present? (nf)
                throw new IllegalArgumentException(String.format(
                        "No model defined for property '%s'", propertyName));
            }
        }

        return createLayerImpl(ctx, configuration);
    }

    // todo - Layer API: why is LayerContext not used in implementations? (mp)
    protected abstract Layer createLayerImpl(LayerContext ctx, ValueContainer configuration);

    // todo - Layer API: why not use annotations? (nf)
    // todo - Layer API: check ALT+F7: is this a utility or framework API? Only framework usage is in createLayer(). How must clients use this? (nf)
    // todo - Layer API: shouldn't it be createLayerConfiguration(LayerContext ctx)? (nf)
    // todo - Layer API: how can clients know whether my value model can be serialized or not? when to impl. a converter? (nf)
    public abstract ValueContainer getConfigurationTemplate();

    // todo - Layer API: check ALT+F7: is this a utility or framework API? move to BEAM Session?  (nf)
    public ValueContainer getConfigurationCopy(LayerContext ctx, Layer layer) {
        final ValueContainer configuration = new ValueContainer();

        for (ValueModel model : layer.getConfiguration().getModels()) {
            final ValueDescriptor descriptor = new ValueDescriptor(model.getDescriptor());
            final DefaultValueAccessor valueAccessor = new DefaultValueAccessor();
            valueAccessor.setValue(model.getValue());
            configuration.addModel(new ValueModel(descriptor, valueAccessor));
        }

        return configuration;
    }

    // todo - Layer API: check ALT+F7: Has no framework usage. (nf)
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

    // todo - Layer API: check following createDefaultValueModel helpers:
    // (1) why "default"? why static if protected? should be non-static for override.
    // (2) check ALT+F7: no framework usage
    protected static ValueModel createDefaultValueModel(String propertyName, Class<?> type) {
        final ValueDescriptor descriptor = new ValueDescriptor(propertyName, type);
        return new ValueModel(descriptor, new DefaultValueAccessor());
    }

    protected static <T> ValueModel createDefaultValueModel(String propertyName, Class<T> type, T defaultValue) {
        final ValueDescriptor descriptor = new ValueDescriptor(propertyName, type);
        descriptor.setDefaultValue(defaultValue);
        descriptor.setNotNull(true);

        final DefaultValueAccessor accessor = new DefaultValueAccessor();
        accessor.setValue(defaultValue);

        return new ValueModel(descriptor, accessor);
    }
}
