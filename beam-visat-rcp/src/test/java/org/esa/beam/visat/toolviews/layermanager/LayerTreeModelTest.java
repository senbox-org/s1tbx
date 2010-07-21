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
package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.LayerUtils;
import junit.framework.TestCase;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import java.util.List;


public class LayerTreeModelTest extends TestCase {

    public void testIt() {
        Layer layer0 = new CollectionLayer();
        Layer layer1 = new CollectionLayer();
        Layer layer2 = new CollectionLayer();
        Layer layer3 = new CollectionLayer();
        layer0.getChildren().add(layer1);
        layer0.getChildren().add(layer2);
        layer0.getChildren().add(layer3);

        Layer layer4 = new CollectionLayer();
        Layer layer5 = new CollectionLayer();
        layer3.getChildren().add(layer4);
        layer3.getChildren().add(layer5);

        LayerTreeModel treeModel = new LayerTreeModel(layer0);

        assertSame(layer0, treeModel.getRoot());

        Layer[] path = LayerUtils.getLayerPath(layer0, layer0);
        assertNotNull(path);
        assertEquals(1, path.length);
        assertSame(layer0, path[0]);

        path = LayerUtils.getLayerPath(layer3, layer4);
        assertNotNull(path);
        assertEquals(2, path.length);
        assertSame(layer3, path[0]);
        assertSame(layer4, path[1]);

        path = LayerUtils.getLayerPath(layer0, layer4);
        assertNotNull(path);
        assertEquals(3, path.length);
        assertSame(layer0, path[0]);
        assertSame(layer3, path[1]);
        assertSame(layer4, path[2]);

        path = LayerUtils.getLayerPath(layer4, layer3);
        assertNotNull(path);
        assertEquals(0, path.length);

        assertEquals(3, treeModel.getChildCount(layer0));
        assertSame(layer1, treeModel.getChild(layer0, 0));
        assertSame(layer2, treeModel.getChild(layer0, 1));
        assertSame(layer3, treeModel.getChild(layer0, 2));

        assertEquals(0, treeModel.getChildCount(layer1));
        assertEquals(0, treeModel.getChildCount(layer2));
        assertEquals(2, treeModel.getChildCount(layer3));
        assertSame(layer4, treeModel.getChild(layer3, 0));
        assertSame(layer5, treeModel.getChild(layer3, 1));


        final MyTreeModelListener listener = new MyTreeModelListener();
        treeModel.addTreeModelListener(listener);
        final List<Layer> children = layer3.getChildren();
        children.remove(layer4);
        assertEquals("treeStructureChanged;", listener.trace);
    }

    private static class MyTreeModelListener implements TreeModelListener {

        String trace = "";
        TreeModelEvent e;

        public void treeNodesChanged(TreeModelEvent e) {
            trace += "treeNodesChanged;";
            this.e = e;
        }

        public void treeNodesInserted(TreeModelEvent e) {
            trace += "treeNodesInserted;";
            this.e = e;
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            trace += "treeNodesRemoved;";
            this.e = e;
        }

        public void treeStructureChanged(TreeModelEvent e) {
            trace += "treeStructureChanged;";
            this.e = e;
        }
    }
}
