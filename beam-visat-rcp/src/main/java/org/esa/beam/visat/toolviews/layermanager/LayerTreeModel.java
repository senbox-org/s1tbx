package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import com.bc.ceres.glayer.support.LayerUtils;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.util.WeakHashMap;

class LayerTreeModel implements TreeModel {

    private final Layer rootLayer;
    private final WeakHashMap<TreeModelListener, Object> treeModelListeners;

    LayerTreeModel(final Layer rootLayer) {
        this.rootLayer = rootLayer;
        this.rootLayer.addListener(new LayerListener());
        treeModelListeners = new WeakHashMap<TreeModelListener, Object>();
    }

    ///////////////////////////////////////////////////////////////////////////
    //  TreeModel interface

    @Override
    public Object getRoot() {
        return rootLayer;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return ((Layer) parent).getChildren().get(index);
    }

    @Override
    public int getChildCount(Object parent) {
        return ((Layer) parent).getChildren().size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((Layer) node).getChildren().isEmpty();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        if (newValue instanceof String) {
            Layer layer = (Layer) path.getLastPathComponent();
            String oldName = layer.getName();
            String newName = (String) newValue;
            if (!oldName.equals(newName)) {
                layer.setName(newName);
                fireTreeNodeChanged(layer);
            }
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((Layer) parent).getChildren().indexOf(child);
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.put(l, "");
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(l);
    }

    //  TreeModel interface
    ///////////////////////////////////////////////////////////////////////////


    public Layer getRootLayer() {
        return rootLayer;
    }

    protected void fireTreeNodeChanged(Layer layer) {
        TreeModelEvent event = createTreeModelEvent(layer);
        for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
            treeModelListener.treeNodesChanged(event);
        }
    }

    protected void fireTreeStructureChanged(Layer parentLayer) {
        TreeModelEvent event = createTreeModelEvent(parentLayer);
        for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
            treeModelListener.treeStructureChanged(event);
        }
    }

    protected void fireTreeNodesInserted(Layer parentLayer) {
        TreeModelEvent event = createTreeModelEvent(parentLayer);
        for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
            treeModelListener.treeNodesInserted(event);
        }
    }

    protected void fireTreeNodesRemoved(Layer parentLayer) {
        TreeModelEvent event = createTreeModelEvent(parentLayer);
        for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
            treeModelListener.treeNodesRemoved(event);
        }
    }

    private TreeModelEvent createTreeModelEvent(Layer layer) {
        Layer[] parentPath = LayerUtils.getLayerPath(rootLayer, layer);
        return new TreeModelEvent(this, parentPath);
    }

    private class LayerListener extends AbstractLayerListener {

        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
            fireTreeNodeChanged(layer);
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            fireTreeNodeChanged(layer);
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            fireTreeStructureChanged(parentLayer);
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            fireTreeStructureChanged(parentLayer);
        }
    }
}