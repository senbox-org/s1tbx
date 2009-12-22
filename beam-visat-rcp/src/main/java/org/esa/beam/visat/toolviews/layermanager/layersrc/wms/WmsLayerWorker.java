package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;

import java.awt.Dimension;
import java.util.concurrent.ExecutionException;

class WmsLayerWorker extends WmsWorker {

    private final Layer rootLayer;

    WmsLayerWorker(LayerSourcePageContext pageContext, RasterDataNode raster) {
        super(pageContext, getFinalImageSize(raster));
        this.rootLayer = pageContext.getLayerContext().getRootLayer();
    }

    @Override
    protected void done() {
        try {
            Layer layer = get();
            try {
                final AppContext appContext = getContext().getAppContext();
                ProductSceneView sceneView = appContext.getSelectedProductSceneView();
                rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), layer);
            } catch (Exception e) {
                getContext().showErrorDialog(e.getMessage());
            }

        } catch (ExecutionException e) {
            getContext().showErrorDialog(
                    String.format("Error while expecting WMS response:\n%s", e.getCause().getMessage()));
        } catch (InterruptedException ignored) {
            // ok
        }
    }

    private static Dimension getFinalImageSize(RasterDataNode raster) {
        int width;
        int height;
        double ratio = raster.getSceneRasterWidth() / (double) raster.getSceneRasterHeight();
        if (ratio >= 1.0) {
            width = Math.min(1280, raster.getSceneRasterWidth());
            height = (int) Math.round(width / ratio);
        } else {
            height = Math.min(1280, raster.getSceneRasterHeight());
            width = (int) Math.round(height * ratio);
        }
        return new Dimension(width, height);
    }
}
