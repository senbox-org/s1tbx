package com.bc.ceres.glayer;

import junit.framework.TestCase;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.glayer.support.ImageLayer;

import java.util.Set;
import java.util.ServiceLoader;
import java.util.Iterator;

public class LayerTypeTest extends TestCase {
    public void testDefaultRegistration() {
        assertTrue(LayerType.getLayerType(Layer.Type.class.getName()) instanceof Layer.Type);
        assertTrue(LayerType.getLayerType(ImageLayer.Type.class.getName()) instanceof ImageLayer.Type);
    }
}
