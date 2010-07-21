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

package org.esa.beam.visat.actions;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayerFilterFactory;
import org.esa.beam.visat.VisatApp;

import java.util.List;

public class ShowGeometryOverlayAction extends AbstractShowOverlayAction {
    private final LayerFilter geometryFilter = VectorDataLayerFilterFactory.createGeometryFilter();

    @Override
    public void actionPerformed(CommandEvent event) {
        ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            List<Layer> childLayers = getGeometryLayers(sceneView);
            for (Layer layer : childLayers) {
                layer.setVisible(isSelected());
            }
        }
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        List<Layer> childLayers = getGeometryLayers(view);
        setEnabled(!childLayers.isEmpty());
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        List<Layer> childLayers = getGeometryLayers(view);
        boolean selected = false;
        for (Layer layer : childLayers) {
            if (layer.isVisible()) {
                selected = true;
                break;
            }
        }
        setSelected(selected);
    }

    private List<Layer> getGeometryLayers(ProductSceneView sceneView) {
        return LayerUtils.getChildLayers(sceneView.getRootLayer(), LayerUtils.SEARCH_DEEP, geometryFilter);
    }

}
