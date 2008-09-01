package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

class LayerTreeModel extends DefaultTreeModel {


    public LayerTreeModel(Layer layer) {
        super(createTreeNodes(layer));
    }

    @Override
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        if (newChild instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) newChild;
            final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parent;
            final Object userObject = parentNode.getUserObject();
            if (userObject instanceof Layer) {
                Layer layer = (Layer) userObject;
                layer.getChildLayerList().add(index, (Layer) treeNode.getUserObject());
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
            if (userObject instanceof Layer) {
                Layer collectionLayer = (Layer) userObject;
                //noinspection SuspiciousMethodCalls
                if (collectionLayer.getChildLayerList().remove(treeNode.getUserObject())) {
                    super.removeNodeFromParent(node);
                }
            }
        }

    }

    private static MutableTreeNode createTreeNodes(Layer layer) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(layer);

        for (int i = 0; i < layer.getChildLayerList().size(); ++i) {
            final Layer childLayer = layer.getChildLayerList().get(i);

            if (layer.getChildLayerList().isEmpty()) {
                node.add(new DefaultMutableTreeNode(childLayer));
            } else {
                node.add(createTreeNodes(childLayer));
            }
        }

        return node;
    }
}
