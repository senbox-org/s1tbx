package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.glayer.LayerType;
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
        return LayerType.getLayerType(getLayerTypeClass().getName());
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

    protected static void ensurePropertyIsDefined(ValueContainer template, String name, Class<?> type) {
        final ValueModel model = template.getModel(name);
        org.junit.Assert.assertNotNull(model);

        final ValueDescriptor descriptor = model.getDescriptor();
        org.junit.Assert.assertNotNull(descriptor);
        org.junit.Assert.assertEquals(type, descriptor.getType());
        org.junit.Assert.assertNotNull(descriptor.getDefaultValue());
        org.junit.Assert.assertNotNull(model.getValue());
    }

    protected static void ensurePropertyIsDeclaredButNotDefined(ValueContainer template, String name, Class<?> type) {
        final ValueModel model = template.getModel(name);
        org.junit.Assert.assertNotNull(model);

        final ValueDescriptor descriptor = model.getDescriptor();
        org.junit.Assert.assertNotNull(descriptor);
        org.junit.Assert.assertEquals(type, descriptor.getType());
        org.junit.Assert.assertNull(descriptor.getDefaultValue());
        org.junit.Assert.assertNull(model.getValue());
    }
}
