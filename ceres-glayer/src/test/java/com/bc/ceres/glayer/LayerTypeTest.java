package com.bc.ceres.glayer;

import com.bc.ceres.glayer.support.ImageLayer;

import junit.framework.TestCase;

public class LayerTypeTest extends TestCase {
    public void testDefaultRegistration() {
        assertTrue(LayerType.getLayerType(CollectionLayer.Type.class.getName()) instanceof CollectionLayer.Type);
        assertTrue(LayerType.getLayerType(ImageLayer.Type.class.getName()) instanceof ImageLayer.Type);
    }
}
