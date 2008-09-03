package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.Enumeration;
import java.util.List;

class LayerTreeModel extends DefaultTreeModel {

    private final DeactivatableLayerListener layerListener;

    public LayerTreeModel(final Layer layer) {
        super(createTreeNodes(layer));

        layerListener = new DeactivatableLayerListener();
        layer.addListener(layerListener);
    }

    @Override
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        if (newChild instanceof DefaultMutableTreeNode) {
            final DefaultMutableTreeNode newChildNode = (DefaultMutableTreeNode) newChild;
            final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) parent;
            final Object parentUserObject = parentNode.getUserObject();

            if (parentUserObject instanceof Layer) {
                final Layer parentLayer = (Layer) parentUserObject;
                final Layer newChildLayer = (Layer) newChildNode.getUserObject();

                super.insertNodeInto(newChild, parent, index);
                synchronized (layerListener) {
                    layerListener.setActive(false);
                    parentLayer.getChildLayerList().add(index, newChildLayer);
                    layerListener.setActive(true);
                }
            }
        }
    }

    @Override
    public void removeNodeFromParent(MutableTreeNode node) {
        if (node instanceof DefaultMutableTreeNode) {
            final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
            final Object parentUserObject = parentNode.getUserObject();

            if (parentUserObject instanceof Layer) {
                super.removeNodeFromParent(node);

                final Layer parentLayer = (Layer) parentUserObject;
                final List<Layer> childLayerList = parentLayer.getChildLayerList();
                final Object childUserObject = ((DefaultMutableTreeNode) node).getUserObject();

                //noinspection SuspiciousMethodCalls
                if (childLayerList.contains(childUserObject)) {
                    synchronized (layerListener) {
                        layerListener.setActive(false);
                        //noinspection SuspiciousMethodCalls
                        childLayerList.remove(childUserObject);
                        layerListener.setActive(true);
                    }
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

    private class DeactivatableLayerListener extends AbstractLayerListener {
        private boolean active;

        private DeactivatableLayerListener() {
            active = true;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            if (active) {
                final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) LayerTreeModel.this.getRoot();
                //noinspection unchecked
                final Enumeration<DefaultMutableTreeNode> nodeEnumeration = rootNode.preorderEnumeration();

                while (nodeEnumeration.hasMoreElements()) {
                    final DefaultMutableTreeNode parentNode = nodeEnumeration.nextElement();

                    if (parentNode.getUserObject() == parentLayer) {
                        insertNodesIntoTreeModel(parentLayer, childLayers, parentNode);
                        break;
                    }
                }
            }
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            if (active) {
                final DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) LayerTreeModel.this.getRoot();
                //noinspection unchecked
                final Enumeration<DefaultMutableTreeNode> nodeEnumeration = rootNode.preorderEnumeration();

                while (nodeEnumeration.hasMoreElements()) {
                    final DefaultMutableTreeNode parentNode = nodeEnumeration.nextElement();

                    if (parentNode.getUserObject() == parentLayer) {
                        removeNodesFromTreeModel(childLayers, parentNode);
                        break;
                    }
                }
            }
        }

        private void insertNodesIntoTreeModel(Layer parentLayer, Layer[] addedLayers,
                                              DefaultMutableTreeNode parentNode) {
            for (final Layer addedLayer : addedLayers) {
                final List<Layer> childLayerList = parentLayer.getChildLayerList();

                for (int i = 0; i < childLayerList.size(); ++i) {
                    if (childLayerList.get(i) == addedLayer) {
                        LayerTreeModel.super.insertNodeInto(new DefaultMutableTreeNode(addedLayer), parentNode, i);
                        break;
                    }
                }
            }
        }

        private void removeNodesFromTreeModel(Layer[] childLayers, DefaultMutableTreeNode parentNode) {
            for (final Layer childLayer : childLayers) {
                //noinspection unchecked
                final Enumeration<DefaultMutableTreeNode> childNodeEnumeration = parentNode.children();

                while (childNodeEnumeration.hasMoreElements()) {
                    final DefaultMutableTreeNode childNode = childNodeEnumeration.nextElement();

                    if (childNode.getUserObject() == childLayer) {
                        LayerTreeModel.super.removeNodeFromParent(childNode);
                        break;
                    }
                }
            }
        }
    }
}
