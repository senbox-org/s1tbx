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

package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;

import java.util.concurrent.ExecutionException;

class ShapefileLayerLoader extends ShapefileLoader {

    ShapefileLayerLoader(LayerSourcePageContext context) {
        super(context);
    }

    @Override
    protected void done() {
        try {
            final Layer layer = get();
            ProductSceneView sceneView = getContext().getAppContext().getSelectedProductSceneView();
            final Layer rootLayer = getContext().getLayerContext().getRootLayer();
            rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), layer);
        } catch (InterruptedException ignored) {
        } catch (ExecutionException e) {
            getContext().showErrorDialog("Could not load shape file: \n" + e.getMessage());
            e.printStackTrace();
        }

    }
}
