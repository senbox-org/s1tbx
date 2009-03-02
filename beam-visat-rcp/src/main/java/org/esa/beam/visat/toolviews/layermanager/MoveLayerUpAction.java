package org.esa.beam.visat.toolviews.layermanager;

import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

import com.bc.ceres.glayer.Layer;

/**
*
* @author Marco Peters
* @version $Revision: $ $Date: $
* @since BEAM 4.6
*/
class MoveLayerUpAction extends AbstractAction {

    private final AppContext appContext;

    MoveLayerUpAction(AppContext appContext) {
        super("Move Layer Up", UIUtils.loadImageIcon("icons/Up24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        final Layer selectedLayer = appContext.getSelectedProductSceneView().getSelectedLayer();
        Layer rootLayer = appContext.getSelectedProductSceneView().getRootLayer();
        if (selectedLayer != null && rootLayer != selectedLayer) {
            moveUp(selectedLayer);
        }
    }

    void moveUp(Layer layer) {
        final Layer parentLayer = layer.getParent();
        final int layerIndex = layer.getParent().getChildIndex(layer.getId());

        if (layerIndex > 0) {
            parentLayer.getChildren().remove(layer);
            parentLayer.getChildren().add(layerIndex - 1, layer);
        }
    }

}
