package org.esa.beam.visat.toolviews.layermanager.layersrc;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.binding.PropertyContainer;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;

/**
 * This layer source uses the given layer type to construct new layer.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Marco Peters
 * @version $ Revision $ $ Date $
 * @since BEAM 4.6
 */
public class SimpleLayerSource implements LayerSource {

    private LayerType layerType;

    public SimpleLayerSource(LayerType layerType) {
        this.layerType = layerType;
    }

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        return layerType.isValidFor(pageContext.getLayerContext());
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
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return true;
    }

    @Override
    public boolean performFinish(LayerSourcePageContext pageContext) {
        LayerContext layerCtx = pageContext.getLayerContext();

        Layer layer = layerType.createLayer(layerCtx, new PropertyContainer());
        if (layer != null) {
            layerCtx.getRootLayer().getChildren().add(layer);
            return true;
        }
        return false;
    }

    @Override
    public void cancel(LayerSourcePageContext pageContext) {
    }
}
