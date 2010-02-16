package org.esa.beam.visat.toolviews.layermanager.layersrc.windfield;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.layer.LayerSource;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;

/**
 * A source for {@link WindFieldLayer}s.
 *
 * @author Norman Fomferra
 * @since BEAM 4.6
 */
public class WindFieldLayerSource implements LayerSource {
    private static final String WINDU_NAME = "zonal_wind";
    private static final String WINDV_NAME = "merid_wind";

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        final Product product = pageContext.getAppContext().getSelectedProduct();
        final RasterDataNode windu = product.getRasterDataNode(WINDU_NAME);
        final RasterDataNode windv = product.getRasterDataNode(WINDV_NAME);
        return windu != null && windv != null;
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
        final Product product = pageContext.getAppContext().getSelectedProduct();
        final RasterDataNode windu = product.getRasterDataNode(WINDU_NAME);
        final RasterDataNode windv = product.getRasterDataNode(WINDV_NAME);
        final WindFieldLayer fieldLayer = WindFieldLayerType.createLayer(windu, windv);
        pageContext.getLayerContext().getRootLayer().getChildren().add(0, fieldLayer);
        return true;
    }

    @Override
    public void cancel(LayerSourcePageContext pageContext) {
    }
}
