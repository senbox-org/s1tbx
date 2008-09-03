/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import junit.framework.TestCase;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * Tests for class {@link LayerTreeModel}.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class LayerTreeModelTest extends TestCase {

    private Layer rootLayer;
    private Layer ernie;
    private Layer bert;
    private Layer bibo;
    private Layer grobi;

    public void testLayerAddedWhenNodeInserted() {
        final LayerTreeModel layerTreeModel = new LayerTreeModel(rootLayer);
        final MutableTreeNode rootNode = (DefaultMutableTreeNode) layerTreeModel.getRoot();

        assertEquals(3, layerTreeModel.getChildCount(rootNode));

        layerTreeModel.insertNodeInto(new DefaultMutableTreeNode(grobi), rootNode, 1);

        assertEquals(4, layerTreeModel.getChildCount(rootNode));
        assertSame(ernie, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 0)).getUserObject());
        assertSame(grobi, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 1)).getUserObject());
        assertSame(bert, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 2)).getUserObject());
        assertSame(bibo, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 3)).getUserObject());

        assertEquals(4, rootLayer.getChildLayerList().size());
        assertSame(ernie, rootLayer.getChildLayerList().get(0));
        assertSame(grobi, rootLayer.getChildLayerList().get(1));
        assertSame(bert, rootLayer.getChildLayerList().get(2));
        assertSame(bibo, rootLayer.getChildLayerList().get(3));
    }

    public void testNodeInsertedWhenLayerAdded() {
        final LayerTreeModel layerTreeModel = new LayerTreeModel(rootLayer);

        final MutableTreeNode rootNode = (DefaultMutableTreeNode) layerTreeModel.getRoot();
        assertEquals(3, layerTreeModel.getChildCount(rootNode));
        assertSame(ernie, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 0)).getUserObject());
        assertSame(bert, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 1)).getUserObject());
        assertSame(bibo, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 2)).getUserObject());

        rootLayer.getChildLayerList().add(1, grobi);

        assertEquals(4, layerTreeModel.getChildCount(rootNode));
        assertSame(ernie, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 0)).getUserObject());
        assertSame(grobi, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 1)).getUserObject());
        assertSame(bert, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 2)).getUserObject());
        assertSame(bibo, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 3)).getUserObject());
    }

    public void testLayerRemovedWhenNodeRemoved() {
        rootLayer.getChildLayerList().add(1, grobi);

        final LayerTreeModel layerTreeModel = new LayerTreeModel(rootLayer);
        final MutableTreeNode rootNode = (DefaultMutableTreeNode) layerTreeModel.getRoot();

        assertEquals(4, layerTreeModel.getChildCount(rootNode));

        layerTreeModel.removeNodeFromParent((MutableTreeNode) rootNode.getChildAt(1));

        assertEquals(3, layerTreeModel.getChildCount(rootNode));
        assertSame(ernie, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 0)).getUserObject());
        assertSame(bert, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 1)).getUserObject());
        assertSame(bibo, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 2)).getUserObject());

        assertEquals(3, rootLayer.getChildLayerList().size());
        assertSame(ernie, rootLayer.getChildLayerList().get(0));
        assertSame(bert, rootLayer.getChildLayerList().get(1));
        assertSame(bibo, rootLayer.getChildLayerList().get(2));
    }

    public void testNodeRemovedWhenLayerRemoved() {
        rootLayer.getChildLayerList().add(1, grobi);

        final LayerTreeModel layerTreeModel = new LayerTreeModel(rootLayer);
        final MutableTreeNode rootNode = (DefaultMutableTreeNode) layerTreeModel.getRoot();

        assertEquals(4, layerTreeModel.getChildCount(rootNode));
        assertSame(ernie, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 0)).getUserObject());
        assertSame(grobi, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 1)).getUserObject());
        assertSame(bert, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 2)).getUserObject());
        assertSame(bibo, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 3)).getUserObject());

        rootLayer.getChildLayerList().remove(1);

        assertEquals(3, layerTreeModel.getChildCount(rootNode));
        assertSame(ernie, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 0)).getUserObject());
        assertSame(bert, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 1)).getUserObject());
        assertSame(bibo, ((DefaultMutableTreeNode) layerTreeModel.getChild(rootNode, 2)).getUserObject());
    }

    @Override
    public void setUp() {
        rootLayer = new Layer();
        ernie = new Layer();
        bert = new Layer();
        bibo = new Layer();
        grobi = new Layer();

        rootLayer.setName("Root");
        ernie.setName("Erni");
        bert.setName("Bert");
        bibo.setName("Bibo");
        grobi.setName("Grobi");

        rootLayer.getChildLayerList().add(ernie);
        rootLayer.getChildLayerList().add(bert);
        rootLayer.getChildLayerList().add(bibo);
    }
}
