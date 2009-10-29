package org.esa.beam.glayer;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.Property;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;

import org.junit.Test;

public abstract class LayerTypeTest {

    private final Class<? extends LayerType> layerTypeClass;

    protected LayerTypeTest(Class<? extends LayerType> layerTypeClass) {
        this.layerTypeClass = layerTypeClass;
    }

    public Class<? extends LayerType> getLayerTypeClass() {
        return layerTypeClass;
    }

    public LayerType getLayerType() {
        return LayerTypeRegistry.getLayerType(getLayerTypeClass());
    }

    @Test
    public void testLayerType() {
        final LayerType layerType = getLayerType();

        org.junit.Assert.assertNotNull(layerType);
        org.junit.Assert.assertTrue(layerTypeClass.isAssignableFrom(layerType.getClass()));
    }

    @Test
    public void testCreateLayerWithNullConfiguration() {
        final LayerType layerType = getLayerType();

        try {
            layerType.createLayer(null, null);
            org.junit.Assert.fail();
        } catch (NullPointerException expected) {
        }
    }

    protected static void ensurePropertyIsDefined(PropertyContainer template, String name, Class<?> type) {
        final Property model = template.getProperty(name);
        org.junit.Assert.assertNotNull(model);

        final PropertyDescriptor descriptor = model.getDescriptor();
        org.junit.Assert.assertNotNull(descriptor);
        org.junit.Assert.assertEquals(type, descriptor.getType());
        org.junit.Assert.assertNotNull(descriptor.getDefaultValue());
        org.junit.Assert.assertNotNull(model.getValue());
    }

    protected static void ensurePropertyIsDeclaredButNotDefined(PropertyContainer template, String name, Class<?> type) {
        final Property model = template.getProperty(name);
        org.junit.Assert.assertNotNull(model);

        final PropertyDescriptor descriptor = model.getDescriptor();
        org.junit.Assert.assertNotNull(descriptor);
        org.junit.Assert.assertEquals(type, descriptor.getType());
        org.junit.Assert.assertNull(descriptor.getDefaultValue());
        org.junit.Assert.assertNull(model.getValue());
    }
}
