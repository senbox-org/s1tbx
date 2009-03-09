package com.bc.ceres.glayer.support;

import junit.framework.TestCase;
import com.bc.ceres.glayer.Layer;

public class LayerUtilsTest extends TestCase {
    public void testGetFirstIndex() {
        Layer root = new Layer("R");
        root.getChildren().add(new Layer("A"));
        root.getChildren().add(new Layer("B"));
        Layer layerC = new Layer("C");
        layerC.getChildren().add(new Layer("C1"));
        layerC.getChildren().add(new Layer("C2"));
        layerC.getChildren().add(new Layer("C3"));
        root.getChildren().add(layerC);
        root.getChildren().add(new Layer("D"));

        assertEquals(0, LayerUtils.getLayerIndex(root, new NameLayerFilter("A"), 0));
        assertEquals(0, LayerUtils.getLayerIndex(root, new NameLayerFilter("A"), -1));
        assertEquals(1, LayerUtils.getLayerIndex(root, new NameLayerFilter("B"), 0));
        assertEquals(2, LayerUtils.getLayerIndex(root, new NameLayerFilter("C3"), 0));
        assertEquals(0, LayerUtils.getLayerIndex(root, new NameLayerFilter("C4"), 0));
        assertEquals(-1, LayerUtils.getLayerIndex(root, new NameLayerFilter("C4"), -1));
    }
}
