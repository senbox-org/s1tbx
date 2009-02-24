package com.bc.ceres.glayer;

import junit.framework.TestCase;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.glayer.support.ImageLayer;

import java.util.Set;
import java.util.ServiceLoader;
import java.util.Iterator;

public class LayerTypeRegistryTest extends TestCase {
    public void testDefaultRegistration() {
        ServiceRegistry<LayerType> r = ServiceRegistryFactory.getInstance().getServiceRegistry(LayerType.class);

        ServiceLoader sl = ServiceLoader.load(LayerType.class);
        Iterator it = sl.iterator();
        while (it.hasNext()) {
            r.addService((LayerType) it.next());
        }

        Set<LayerType> services = r.getServices();
        assertNotNull(services);
        assertTrue(services.size() >= 2);
        assertTrue(r.getService(Layer.Type.class.getName()) instanceof Layer.Type);
        assertTrue(r.getService(ImageLayer.Type.class.getName()) instanceof ImageLayer.Type);

        
    }
}
