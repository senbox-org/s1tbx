package org.esa.beam.visat.toolviews.layermanager.layersrc;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.visat.toolviews.layermanager.LayerSourceController;

import java.util.HashMap;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class SimpleLayerSourceController implements LayerSourceController {

    private LayerType layerType;

    public SimpleLayerSourceController(LayerType layerType) {
        this.layerType = layerType;
    }

    @Override
    public boolean isApplicable(AppAssistantPageContext pageContext) {
        return layerType.isValidFor(pageContext.getAppContext().getSelectedProductSceneView().getLayerContext());
    }

    @Override
    public AbstractAppAssistantPage getFirstPage(AppAssistantPageContext pageContext) {
        return null;
    }

    @Override
    public boolean finish(AppAssistantPageContext pageContext) {
        LayerContext layerCtx = pageContext.getAppContext().getSelectedProductSceneView().getLayerContext();

        Layer layer = layerType.createLayer(layerCtx, new HashMap<String, Object>());
        if (layer != null) {
            layerCtx.getRootLayer().getChildren().add(layer);
            return true;
        }
        return false;
    }

    @Override
    public void cancel() {
    }
}
