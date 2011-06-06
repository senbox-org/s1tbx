package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ServiceRegistry;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 * @since SeaDAS 7.0
 */
public class PlacemarkDescriptorRegistryTest {
    @Test
    public void testThatRegistryHostsKnownDescriptors() throws Exception {
        ServiceRegistry<PlacemarkDescriptor> registry = PlacemarkDescriptorRegistry.getServiceRegistry();
        assertNotNull(registry);
        assertNotNull(registry.getService("org.esa.beam.framework.datamodel.PinDescriptor"));
        assertNotNull(registry.getService("org.esa.beam.framework.datamodel.GcpDescriptor"));
        assertNotNull(registry.getService("org.esa.beam.framework.datamodel.GeometryDescriptor"));
    }
}
