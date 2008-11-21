package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.Enumeration;
import java.util.List;

class LayerTreeModel extends DefaultTreeModel {

    public LayerTreeModel(final Layer layer) {
        super(createTreeNodes(layer));
        layer.addListener(new LayerListener());
    }

    public Layer getRootLayer() {
        return (Layer) ((DefaultMutableTreeNode) getRoot()).getUserObject();
    }

    public Layer getLayer(String name) {
        return getLayer(getRootLayer(), name);
    }

    public Layer getParentLayer(Layer layer) {
        return getParentLayer(getRootLayer(), layer);
    }

    public Layer removeLayer(Layer layer) {
        Layer parentLayer = getParentLayer(layer);
        if (parentLayer != null) {
            parentLayer.getChildren().remove(parentLayer);
        }
        return parentLayer;
    }

    private Layer getLayer(Layer rootLayer, String name) {
        List<Layer> children = rootLayer.getChildren();
        for (Layer child : children) {
            if (child.getName().equalsIgnoreCase(name)) {
                return child;
            }
        }
        for (Layer child : children) {
            Layer layer = getLayer(child, name);
            if (layer != null) {
                return layer;
            }
        }
        return null;
    }

    private Layer getParentLayer(Layer rootLayer, Layer layer) {
        List<Layer> children = rootLayer.getChildren();
        if (children.contains(layer)) {
            return rootLayer;
        }
        for (Layer child : children) {
            Layer parentLayer = getParentLayer(child, layer);
            if (parentLayer != null) {
                return parentLayer;
            }
        }
        return null;
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

                if (!containsLayer(parentNode, newChildLayer)) {
                    super.insertNodeInto(newChild, parent, index);
                    if (!parentLayer.getChildren().contains(newChildLayer)) {
                        parentLayer.getChildren().add(index, newChildLayer);
                    }
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
                final List<Layer> childLayerList = parentLayer.getChildren();
                final Object childUserObject = ((DefaultMutableTreeNode) node).getUserObject();

                //noinspection SuspiciousMethodCalls
                if (childLayerList.contains(childUserObject)) {
                    //noinspection SuspiciousMethodCalls
                    childLayerList.remove(childUserObject);
                }
            }
        }
    }

    public static DefaultMutableTreeNode getLayerNode(DefaultMutableTreeNode parentNode, String layerName) {
        if (isLayerNode(parentNode, layerName)) {
            return parentNode;
        }
        //noinspection unchecked
        final Enumeration<DefaultMutableTreeNode> enumeration = parentNode.children();
        while (enumeration.hasMoreElements()) {
            DefaultMutableTreeNode childNode = enumeration.nextElement();
            DefaultMutableTreeNode someNode = getLayerNode(childNode, layerName);
            if (someNode != null) {
                return someNode;
            }
        }
        return null;
    }

    private static boolean isLayerNode(DefaultMutableTreeNode parentNode, String layerName) {
        Object o = parentNode.getUserObject();
        return o instanceof Layer && ((Layer) o).getName().equalsIgnoreCase(layerName);
    }

    public static boolean containsLayer(DefaultMutableTreeNode parentNode, Layer newChildLayer) {
        //noinspection unchecked
        final Enumeration<DefaultMutableTreeNode> enumeration = parentNode.children();
        while (enumeration.hasMoreElements()) {
            if (enumeration.nextElement().getUserObject() == newChildLayer) {
                return true;
            }
        }
        return false;
    }

    public static MutableTreeNode createTreeNodes(Layer layer) {
        final DefaultMutableTreeNode node = new DefaultMutableTreeNode(layer);

        for (final Layer childLayer : layer.getChildren()) {
            if (layer.getChildren().isEmpty()) {
                node.add(new DefaultMutableTreeNode(childLayer));
            } else {
                node.add(createTreeNodes(childLayer));
            }
        }

        return node;
    }

    private class LayerListener extends AbstractLayerListener {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
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

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
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

        private void insertNodesIntoTreeModel(Layer parentLayer, Layer[] addedLayers,
                                              DefaultMutableTreeNode parentNode) {
            for (final Layer addedLayer : addedLayers) {
                final List<Layer> childLayerList = parentLayer.getChildren();

                for (int i = 0; i < childLayerList.size(); ++i) {
                    if (childLayerList.get(i) == addedLayer) {
                        LayerTreeModel.this.insertNodeInto(new DefaultMutableTreeNode(addedLayer), parentNode, i);
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
                        LayerTreeModel.this.removeNodeFromParent(childNode);
                        break;
                    }
                }
            }
        }
    }
}
