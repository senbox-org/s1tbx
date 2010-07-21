/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
