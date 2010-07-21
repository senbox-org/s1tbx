/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.filters.NameFilter;
import junit.framework.TestCase;

public class LayerUtilsTest extends TestCase {

    static Layer createLayerTree() {
        Layer root = new CollectionLayer("R");
        root.getChildren().add(new CollectionLayer("A"));
        root.getChildren().add(new CollectionLayer("B"));
        root.getChildren().add(new CollectionLayer("C"));
        root.getChildren().add(new CollectionLayer("D"));

        Layer layerC = LayerUtils.getChildLayerByName(root, "C");
        assertNotNull(layerC);
        layerC.getChildren().add(new CollectionLayer("C1"));
        layerC.getChildren().add(new CollectionLayer("C2"));
        layerC.getChildren().add(new CollectionLayer("C3"));
        return root;
    }

    public void testGetChildLayerIndex() {
        Layer root = createLayerTree();

        assertEquals(0, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("A")));
        assertEquals(1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("B")));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("C")));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("C1")));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("C2")));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("C3")));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("C4")));
        assertEquals(3, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("D")));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -1, new NameFilter("E")));

        assertEquals(0, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("A")));
        assertEquals(1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("B")));
        assertEquals(2, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("C")));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("C1")));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("C2")));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("C3")));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("C4")));
        assertEquals(3, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("D")));
        assertEquals(-1, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.FLAT, -1, new NameFilter("E")));

        assertEquals(-999, LayerUtils.getChildLayerIndex(root, LayerUtils.SearchMode.DEEP, -999, new NameFilter("E")));

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


    public void testGetRootLayer() {
        Layer root = createLayerTree();
        Layer directRoot = LayerUtils.getRootLayer(root);
        assertSame(root, directRoot);

        Layer layerC = LayerUtils.getChildLayerByName(root, "C");
        Layer cRoot = LayerUtils.getRootLayer(layerC);
        assertSame(root, cRoot);

        Layer layerC2 = LayerUtils.getChildLayerByName(root, "C2");
        Layer c2Root = LayerUtils.getRootLayer(layerC2);
        assertSame(root, c2Root);
    }
}
