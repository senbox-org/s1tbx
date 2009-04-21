package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.visat.toolviews.layermanager.layersrc.wms.WmsLayerSource;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

public class DefaultLayerSourceDescriptorTest {

    private static SimpleTestLayerType layerType = new SimpleTestLayerType();

    @BeforeClass
    public static void setUp() {
        final ServiceRegistryFactory serviceRegistryFactory = ServiceRegistryFactory.getInstance();
        final ServiceRegistry<LayerType> registry = serviceRegistryFactory.getServiceRegistry(LayerType.class);
        registry.addService(layerType);
    }

    @AfterClass
    public static void tearDown() {
        final ServiceRegistryFactory serviceRegistryFactory = ServiceRegistryFactory.getInstance();
        final ServiceRegistry<LayerType> registry = serviceRegistryFactory.getServiceRegistry(LayerType.class);
        registry.removeService(layerType);
    }

    @Test
    public void testWithLayerSource() {

        final String id = "wms-layer-source";
        final String name = "Image from Web Mapping Server (WMS)";
        final String description = "Retrieves images from a Web Mapping Server (WMS)";
        DefaultLayerSourceDescriptor descriptor = new DefaultLayerSourceDescriptor(id, name, description,
                                                                                   WmsLayerSource.class);
        assertEquals("wms-layer-source", descriptor.getId());
        assertEquals("Image from Web Mapping Server (WMS)", descriptor.getName());
        assertEquals("Retrieves images from a Web Mapping Server (WMS)", descriptor.getDescription());
        assertNull(descriptor.getLayerType());

        final LayerSource layerSource = descriptor.createLayerSource();
        assertNotNull(layerSource);
        assertTrue(layerSource instanceof WmsLayerSource);
    }

    @Test
    public void testWithLayerType() {

        final String id = "type-layer-source";
        final String name = "Some simple layer";
        final String description = "A simple layer without configuration.";
        DefaultLayerSourceDescriptor descriptor = new DefaultLayerSourceDescriptor(id, name, description,
                                                                                   SimpleTestLayerType.class.getName());
        assertEquals("type-layer-source", descriptor.getId());
        assertEquals("Some simple layer", descriptor.getName());
        assertEquals("A simple layer without configuration.", descriptor.getDescription());
        assertNotNull(descriptor.getLayerType());
        assertNotNull(descriptor.getLayerType() instanceof SimpleTestLayerType);

        final LayerSource layerSource = descriptor.createLayerSource();
        assertNotNull(layerSource);
    }


    private static class SimpleTestLayerType extends LayerType {

        @Override
        public String getName() {
            return "simple layer";
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        protected Layer createLayerImpl(LayerContext ctx, ValueContainer configuration) {
            return new CollectionLayer();
        }

        @Override
        public ValueContainer getConfigurationTemplate() {
            return new ValueContainer();
        }
    }
}
