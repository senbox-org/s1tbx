package com.bc.ceres.glayer;

import com.bc.ceres.glayer.support.ImageLayer;

import junit.framework.TestCase;

public class LayerTypeRegistryTest extends TestCase {
    
    public void testGetLayerTypeByClass() {
        assertNotNull(LayerTypeRegistry.getLayerType(CollectionLayer.Type.class));
        assertNotNull(LayerTypeRegistry.getLayerType(ImageLayer.Type.class));
    }
    
    public void testGetLayerTypeByName() {
        assertNotNull(LayerTypeRegistry.getLayerType(CollectionLayer.Type.class.getName()));
        assertNotNull(LayerTypeRegistry.getLayerType(ImageLayer.Type.class.getName()));
    }
    
    public void testLayerTypeIsOfCorrectType() {
        LayerType collectionLayerType = LayerTypeRegistry.getLayerType(CollectionLayer.Type.class.getName());
        assertTrue(collectionLayerType instanceof CollectionLayer.Type);
        LayerType imageLayerType = LayerTypeRegistry.getLayerType(ImageLayer.Type.class.getName());
        assertTrue(imageLayerType instanceof ImageLayer.Type);
    }
    
    public void testAliases() {
        LayerType imageLayerType = LayerTypeRegistry.getLayerType("ImageLayerType");
        assertTrue(imageLayerType instanceof ImageLayer.Type);
    }
}
