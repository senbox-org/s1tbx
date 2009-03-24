package org.esa.beam.worldmap;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.LayerSourceController;

/**
 * @author Marco Zühlke
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class BlueMarbleLayerSourceController implements LayerSourceController {

    @Override
    public AbstractAppAssistantPage getFirstPage(AppAssistantPageContext pageContext) {
        return null;
    }

    @Override
    public boolean isApplicable(AppAssistantPageContext pageContext) {
        return true;
    }

    @Override
    public boolean finish(AppAssistantPageContext pageContext) {
        ProductSceneView sceneView = pageContext.getAppContext().getSelectedProductSceneView();
        final Layer rootLayer = sceneView.getRootLayer();
        Layer worldMapLayer = BlueMarbleWorldMapLayer.createWorldMapLayer();
        worldMapLayer.setVisible(true);
        rootLayer.getChildren().add(worldMapLayer);
        return true;
    }

    @Override
    public void cancel() {

    }

}
