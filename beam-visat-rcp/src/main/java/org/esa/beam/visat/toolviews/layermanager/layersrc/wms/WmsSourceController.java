/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.layermanager.layersrc.wms;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.toolviews.layermanager.ControllerAssitantPage;
import org.esa.beam.visat.toolviews.layermanager.LayerSourceController;

import javax.media.jai.PlanarImage;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutionException;


public class WmsSourceController implements LayerSourceController {

    private final WmsModel wmsModel = new WmsModel();

    @Override
    public boolean isApplicable(AppAssistantPageContext pageContext) {
        return true;
    }

    @Override
    public boolean hasFirstPage() {
        return true;
    }

    @Override
    public ControllerAssitantPage getFirstPage(AppAssistantPageContext pageContext) {
        return new ControllerAssitantPage(new WmsAssistantPage1(wmsModel), this);
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean finish(AppAssistantPageContext pageContext) {
        ProductSceneView view = pageContext.getAppContext().getSelectedProductSceneView();
        RasterDataNode raster = view.getRaster();

        WmsLayerWorker layerWorker = new WmsLayerWorker(view.getRootLayer(),
                                                        getFinalImageSize(raster),
                                                        wmsModel,
                                                        pageContext);
        layerWorker.execute();   // todo - don't close dialog before image is downloaded! (nf)
        return true;
    }

    private Dimension getFinalImageSize(RasterDataNode raster) {
        int width, height;
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

    private class WmsLayerWorker extends WmsWorker {

        private final com.bc.ceres.glayer.Layer rootLayer;
        private JDialog dialog;
        private JProgressBar progressBar;
        private final AppAssistantPageContext pageContext;

        private WmsLayerWorker(com.bc.ceres.glayer.Layer rootLayer,
                               Dimension size,
                               WmsModel wmsModel,
                               AppAssistantPageContext pageContext) {
            super(size, wmsModel);
            this.rootLayer = rootLayer;
            this.pageContext = pageContext;
            progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            dialog = new JDialog(pageContext.getWindow(), "Loading image from WMS...",
                                 Dialog.ModalityType.DOCUMENT_MODAL);
            dialog.getContentPane().add(progressBar, BorderLayout.SOUTH);
            dialog.pack();
        }

        @Override
        protected BufferedImage doInBackground() throws Exception {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Rectangle parentBounds = pageContext.getWindow().getBounds();
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
                BufferedImage image = get();
                try {
                    ProductSceneView sceneView = pageContext.getAppContext().getSelectedProductSceneView();
                    AffineTransform i2mTransform = sceneView.getRaster().getGeoCoding().getGridToModelTransform();
                    ImageLayer imageLayer = new ImageLayer(PlanarImage.wrapRenderedImage(image), i2mTransform);
                    imageLayer.setName(wmsModel.getSelectedLayer().getName());
                    rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), imageLayer);
                } catch (Exception e) {
                    pageContext.showErrorDialog(e.getMessage());
                }

            } catch (ExecutionException e) {
                pageContext.showErrorDialog(
                        String.format("Error while expecting WMS response:\n%s", e.getCause().getMessage()));
            } catch (InterruptedException ignored) {
                // ok
            }
        }

    }
}
