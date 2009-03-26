package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.DummyTestLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.filters.NameFilter;
import junit.framework.TestCase;

public class LayerUtilsTest extends TestCase {

    static Layer createLayerTree() {
        Layer root = new DummyTestLayer("R");
        root.getChildren().add(new DummyTestLayer("A"));
        root.getChildren().add(new DummyTestLayer("B"));
        root.getChildren().add(new DummyTestLayer("C"));
        root.getChildren().add(new DummyTestLayer("D"));

        Layer layerC = LayerUtils.getChildLayerByName(root, "C");
        assertNotNull(layerC);
        layerC.getChildren().add(new DummyTestLayer("C1"));
        layerC.getChildren().add(new DummyTestLayer("C2"));
        layerC.getChildren().add(new DummyTestLayer("C3"));
        return root;
    }

    public void testGetChildLayerIndex() {
        Layer root = createLayerTree();

        assertEquals(0, LayerUtils.getChildLayerIndex(root, new NameFilter("A"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(1, LayerUtils.getChildLayerIndex(root, new NameFilter("B"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, new NameFilter("C"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, new NameFilter("C1"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, new NameFilter("C2"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, new NameFilter("C3"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, new NameFilter("C4"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(3, LayerUtils.getChildLayerIndex(root, new NameFilter("D"), LayerUtils.SearchMode.DEEP, -1));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, new NameFilter("E"), LayerUtils.SearchMode.DEEP, -1));

        assertEquals(0, LayerUtils.getChildLayerIndex(root, new NameFilter("A"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(1, LayerUtils.getChildLayerIndex(root, new NameFilter("B"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, new NameFilter("C"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, new NameFilter("C1"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, new NameFilter("C2"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, new NameFilter("C3"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, new NameFilter("C4"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(3, LayerUtils.getChildLayerIndex(root, new NameFilter("D"), LayerUtils.SearchMode.FLAT, -1));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, new NameFilter("E"), LayerUtils.SearchMode.FLAT, -1));

        assertEquals(-999, LayerUtils.getChildLayerIndex(root, new NameFilter("E"), LayerUtils.SearchMode.DEEP, -999));

    }

    public void testGetLayerPath() {
        Layer root = createLayerTree();
        Layer layerA = LayerUtils.getChildLayerByName(root, "A");
        Layer layerC = LayerUtils.getChildLayerByName(root, "C");
        Layer layerC2 = LayerUtils.getChildLayerByName(root, "C2");

        Layer[] pathA = LayerUtils.getLayerPath(root, layerA);
        assertNotNull(pathA);
        assertEquals(2, pathA.length);
        assertSame(root, pathA[0]);
        assertSame(layerA, pathA[1]);

        Layer[] pathC2 = LayerUtils.getLayerPath(root, layerC2);
        assertNotNull(pathC2);
        assertEquals(3, pathC2.length);
        assertSame(root, pathC2[0]);
        assertSame(layerC, pathC2[1]);
        assertSame(layerC2, pathC2[2]);
    }

}
