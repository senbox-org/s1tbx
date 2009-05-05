package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
class MoveLayerRightAction extends AbstractAction {

    private final AppContext appContext;

    MoveLayerRightAction(AppContext appContext) {
        super("Move Layer Right", UIUtils.loadImageIcon("icons/Right24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        final Layer selectedLayer = appContext.getSelectedProductSceneView().getSelectedLayer();
        Layer rootLayer = appContext.getSelectedProductSceneView().getRootLayer();
        if (selectedLayer != null && rootLayer != selectedLayer) {
            moveRight(selectedLayer);
        }
    }

    void moveRight(Layer layer) {
        if (canMove(layer)) {
            final Layer parentLayer = layer.getParent();
            final int layerIndex = parentLayer.getChildIndex(layer.getId());
            final Layer targetLayer = parentLayer.getChildren().get(layerIndex - 1);
            parentLayer.getChildren().remove(layer);
            targetLayer.getChildren().add(layer);
        }
    }

    public boolean canMove(Layer layer) {
        final Layer parentLayer = layer.getParent();
        final int layerIndex = parentLayer.getChildIndex(layer.getId());
        if (layerIndex > 0) {
            final Layer targetLayer = parentLayer.getChildren().get(layerIndex - 1);
            if (targetLayer.isCollectionLayer()) {
                return true;
            }
        }
        return false;
    }

}
