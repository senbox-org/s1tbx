package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
class LayerMover {

    private final Layer rootLayer;


    public LayerMover(final Layer rootLayer) {
        this.rootLayer = rootLayer;
    }

    public void moveUp(Layer layer) {
        if (rootLayer == layer) {
            return;
        }
        final Layer[] layerPath = LayerTreeModel.getLayerPath(rootLayer, layer);
        final Layer parentLayer = layerPath[layerPath.length - 2];
        final int layerIndex = parentLayer.getChildIndex(layer.getId());

        if (layerIndex > 0) {
            parentLayer.getChildren().remove(layer);
            parentLayer.getChildren().add(layerIndex - 1, layer);
        }
    }

    public void moveDown(Layer layer) {
        if (rootLayer == layer) {
            return;
        }
        final Layer[] layerPath = LayerTreeModel.getLayerPath(rootLayer, layer);
        final Layer parentLayer = layerPath[layerPath.length - 2];
        final int layerIndex = parentLayer.getChildIndex(layer.getId());

        final boolean isLast = layerIndex == (parentLayer.getChildren().size() - 1);
        if (!isLast) {
            parentLayer.getChildren().remove(layer);
            parentLayer.getChildren().add(layerIndex + 1, layer);
        }
    }

    public void moveLeft(Layer layer) {
        if (rootLayer == layer) {
            return;
        }
        final Layer[] layerPath = LayerTreeModel.getLayerPath(rootLayer, layer);
        final Layer parentLayer = layerPath[layerPath.length - 2];
        if (parentLayer != rootLayer) {
            parentLayer.getChildren().remove(layer);
            final Layer parentsParentLayer = layerPath[layerPath.length - 3];
            final int parentIndex = parentsParentLayer.getChildIndex(parentLayer.getId());
            if (parentIndex < parentsParentLayer.getChildren().size() - 1) {
                parentsParentLayer.getChildren().add(parentIndex + 1, layer);
            } else {
                parentsParentLayer.getChildren().add(layer);
            }
        }
    }

    public void moveRight(Layer layer) {
        if (rootLayer == layer) {
            return;
        }
        final Layer[] layerPath = LayerTreeModel.getLayerPath(rootLayer, layer);
        final Layer parentLayer = layerPath[layerPath.length - 2];

        final int layerIndex = parentLayer.getChildIndex(layer.getId());
        if (layerIndex > 0) {
            final Layer targetLayer = parentLayer.getChildren().get(layerIndex - 1);
            parentLayer.getChildren().remove(layer);
            targetLayer.getChildren().add(layer);
        } 

    }
}
