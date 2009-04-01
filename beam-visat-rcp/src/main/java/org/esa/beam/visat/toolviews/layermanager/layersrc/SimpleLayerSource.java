package org.esa.beam.visat.toolviews.layermanager.layersrc;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;

import java.util.HashMap;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
public class SimpleLayerSource implements LayerSource {

    private LayerType layerType;

    public SimpleLayerSource(LayerType layerType) {
        this.layerType = layerType;
    }

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        return layerType.isValidFor(pageContext.getAppContext().getSelectedProductSceneView().getLayerContext());
    }

    @Override
    public boolean hasFirstPage() {
        return false;
    }

    @Override
    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        return null;
    }

    @Override
    public boolean finish(LayerSourcePageContext pageContext) {
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
