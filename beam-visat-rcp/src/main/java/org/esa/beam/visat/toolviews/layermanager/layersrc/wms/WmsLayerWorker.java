package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;

import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.concurrent.ExecutionException;

class WmsLayerWorker extends WmsWorker {

    private final Layer rootLayer;
    private JDialog dialog;

    WmsLayerWorker(Layer rootLayer,
                   RasterDataNode raster,
                   LayerSourcePageContext pageContext) {
        super(getFinalImageSize(raster), pageContext);
        this.rootLayer = rootLayer;
        dialog = new JDialog(pageContext.getWindow(), "Loading image from WMS...",
                             Dialog.ModalityType.DOCUMENT_MODAL);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        dialog.getContentPane().add(progressBar, BorderLayout.SOUTH);
        dialog.pack();
    }

    @Override
    protected Layer doInBackground() throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Rectangle parentBounds = getContext().getWindow().getBounds();
                Rectangle bounds = dialog.getBounds();
                dialog.setLocation(parentBounds.x + (parentBounds.width - bounds.width) / 2,
                                   parentBounds.y + (parentBounds.height - bounds.height) / 2);
                dialog.setVisible(true);
            }
        });

        return super.doInBackground();
    }

    @Override
    protected void done() {
        dialog.dispose();

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
