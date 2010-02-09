package org.esa.beam.framework.ui.layer;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultLayerSourceDescriptorTest {

    private static SimpleTestLayerType layerType = new SimpleTestLayerType();

    @BeforeClass
    public static void setUp() {
        final ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        final ServiceRegistry<LayerType> registry = serviceRegistryManager.getServiceRegistry(LayerType.class);
        registry.addService(layerType);
    }

    @AfterClass
    public static void tearDown() {
        final ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        final ServiceRegistry<LayerType> registry = serviceRegistryManager.getServiceRegistry(LayerType.class);
        registry.removeService(layerType);
    }

    @Test
    public void testWithLayerSource() {

        final String id = "wms-layer-source";
        final String name = "Image from Web Mapping Server (WMS)";
        final String description = "Retrieves images from a Web Mapping Server (WMS)";
        DefaultLayerSourceDescriptor descriptor = new DefaultLayerSourceDescriptor(id, name, description,
                                                                                   DummyLayerSource.class);
        assertEquals("wms-layer-source", descriptor.getId());
        assertEquals("Image from Web Mapping Server (WMS)", descriptor.getName());
        assertEquals("Retrieves images from a Web Mapping Server (WMS)", descriptor.getDescription());
        assertNull(descriptor.getLayerType());

        final LayerSource layerSource = descriptor.createLayerSource();
        assertNotNull(layerSource);
        assertTrue(layerSource instanceof DummyLayerSource);
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

        private static final String TYPE_NAME = "SimpleTestLayerType";

        @Override
        public String getName() {
            return TYPE_NAME;
        }
        
        @Override
        public boolean isValidFor(LayerContext ctx) {
            return true;
        }

        @Override
        public Layer createLayer(LayerContext ctx, PropertySet configuration) {
            return new CollectionLayer();
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            return new PropertyContainer();
        }
    }

    private static class DummyLayerSource implements LayerSource {

        public DummyLayerSource() {
        }

        @Override
        public boolean isApplicable(LayerSourcePageContext pageContext) {
            return false;
        }

        @Override
        public boolean hasFirstPage() {
            return false;
        }

        @Override
        public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
            return null;
        }

        @Override
        public boolean canFinish(LayerSourcePageContext pageContext) {
            return false;
        }

        @Override
        public boolean performFinish(LayerSourcePageContext pageContext) {
            return false;
        }

        @Override
        public void cancel(LayerSourcePageContext pageContext) {
        }
    }
}
