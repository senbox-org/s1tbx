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
class RemoveLayerAction extends AbstractAction {

    private final AppContext appContext;


    RemoveLayerAction(AppContext appContext) {
        super("Remove Layer", UIUtils.loadImageIcon("icons/Minus24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, RemoveLayerAction.class.getName());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Layer selectedLayer = appContext.getSelectedProductSceneView().getSelectedLayer();
        if (selectedLayer != null) {
            selectedLayer.getParent().getChildren().remove(selectedLayer);
            selectedLayer.dispose();
        }
    }


}
