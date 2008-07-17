package org.esa.beam.pview;

import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.GraphicalLayer;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

class CollectionLayerTreeModel extends DefaultTreeModel {


    public CollectionLayerTreeModel(CollectionLayer collectionLayer) {
        super(createTreeNodes(collectionLayer));
    }

    @Override
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        if (newChild instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) newChild;
            final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parent;
            final Object userObject = parentNode.getUserObject();
            if (userObject instanceof CollectionLayer) {
                CollectionLayer collectionLayer = (CollectionLayer) userObject;
                collectionLayer.add(index, (GraphicalLayer) treeNode.getUserObject());
                super.insertNodeInto(newChild, parent, index);
            }
        }
    }

    @Override
    public void removeNodeFromParent(MutableTreeNode node) {
        if (node instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
            final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) treeNode.getParent();
            final Object userObject = parent.getUserObject();
            if (userObject instanceof CollectionLayer) {
                CollectionLayer collectionLayer = (CollectionLayer) userObject;
                if (collectionLayer.remove((GraphicalLayer) treeNode.getUserObject())) {
                    super.removeNodeFromParent(node);
                }
            }
        }

    }

    private static MutableTreeNode createTreeNodes(CollectionLayer collectionLayer) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(collectionLayer);
        for (GraphicalLayer layer : collectionLayer) {
            if (layer instanceof CollectionLayer) {
                node.add(createTreeNodes((CollectionLayer) layer));
            } else {
                node.add(new DefaultMutableTreeNode(layer));
            }
        }
        return node;
    }
}
