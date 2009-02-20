package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.AbstractLayerListener;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class LayerTreeModel implements TreeModel {

    private final Layer rootLayer;
    private final WeakHashMap<TreeModelListener, Object> treeModelListeners;

    public LayerTreeModel(final Layer rootLayer) {
        this.rootLayer = rootLayer;
        this.rootLayer.addListener(new LayerListener());
        treeModelListeners = new WeakHashMap<TreeModelListener, Object>();
    }

    ///////////////////////////////////////////////////////////////////////////
    //  TreeModel interface

    public Object getRoot() {
        return rootLayer;
    }

    public Object getChild(Object parent, int index) {
        return ((Layer) parent).getChildren().get(index);
    }

    public int getChildCount(Object parent) {
        return ((Layer) parent).getChildren().size();
    }

    public boolean isLeaf(Object node) {
        return ((Layer) node).getChildren().isEmpty();
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        final TreeModelEvent event = new TreeModelEvent(this, path);
        for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
            treeModelListener.treeNodesChanged(event);
        }
    }

    public int getIndexOfChild(Object parent, Object child) {
        return ((Layer) parent).getChildren().indexOf(child);
    }

    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.put(l, "");
    }

    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.remove(l);
    }

//  TreeModel interface
    ///////////////////////////////////////////////////////////////////////////


    public Layer getRootLayer() {
        return rootLayer;
    }

    // Todo - move to Layer API? Or LayerUtils?    
    public static Layer getLayer(Layer rootLayer, String layerId) {
        List<Layer> children = rootLayer.getChildren();
        for (Layer child : children) {
            if (layerId.equalsIgnoreCase(child.getId())) {
                return child;
            }
        }
        for (Layer child : children) {
            Layer layer = getLayer(child, layerId);
            if (layer != null) {
                return layer;
            }
        }
        return null;
    }

    // Todo - move to Layer API? Or LayerUtils?
    public static Layer[] getLayerPath(Layer layer, Layer childLayer) {
        if(layer == childLayer) {
            return new Layer[]{layer};
        }
        final ArrayList<Layer> layerArrayList = new ArrayList<Layer>();
        collectLayerPath(layer, childLayer, layerArrayList);
        return layerArrayList.toArray(new Layer[layerArrayList.size()]);
    }

    private static boolean collectLayerPath(Layer layer, Layer childLayer, List<Layer> collection) {
        List<Layer> children = layer.getChildren();
        if (children.contains(childLayer)) {
            collection.add(layer);
            collection.add(childLayer);
            return true;
        }
        for (Layer child : children) {
            if (collectLayerPath(child, childLayer, collection)) {
                collection.add(0, layer);
                return true;
            }
        }
        return false;
    }

    private class LayerListener extends AbstractLayerListener {
        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
            Layer[] parentPath = getLayerPath(rootLayer, parentLayer);
            final int[] childIndexes = getChildIndexes(parentLayer, childLayers);
            TreeModelEvent event = new TreeModelEvent(LayerTreeModel.this, parentPath, childIndexes, childLayers);
            for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
                treeModelListener.treeStructureChanged(event);
                // TODO - Why does treeNodesInserted() not work? (mp - 20.02.09)
                // treeModelListener.treeNodesInserted(event);
            }
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
            Layer[] parentPath = getLayerPath(rootLayer, parentLayer);
            final int[] childIndexes = getChildIndexes(parentLayer, childLayers);
            TreeModelEvent event = new TreeModelEvent(LayerTreeModel.this, parentPath, childIndexes, childLayers);
            for (TreeModelListener treeModelListener : treeModelListeners.keySet()) {
                treeModelListener.treeStructureChanged(event);
                // TODO - Why does treeNodesRemoved() not work? (mp - 20.02.09)
//                 treeModelListener.treeNodesRemoved(event);
            }
        }

        private int[] getChildIndexes(Layer parentLayer, Layer[] childLayers) {
            final int[] childIndices = new int[childLayers.length];
            for (int i = 0; i < childLayers.length; i++) {
                childIndices[i] = getIndexOfChild(parentLayer, childLayers[i]);
            }
            return childIndices;
        }

    }
}