/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.layers;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.figure.interactions.SelectionInteractor;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

public class SelectLayerInteractor extends SelectionInteractor {

    public SelectLayerInteractor() {

    }

    protected boolean isMouseOverSelection(MouseEvent event) {
        final boolean figureSelected = super.isMouseOverSelection(event);
        final boolean layerSelected = false;
        return figureSelected || layerSelected;
    }

    public SelectRectangleTool createSelectRectangleTool() {
        return new SelectLayerRectangleTool();        
    }

    public SelectPointTool createSelectPointTool() {
        return new SelectLayerPointTool();
    }

    private class SelectLayerPointTool extends SelectPointTool {
        @Override
        public void end(MouseEvent event) {
            super.end(event);

            final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
            final List<Layer> layers = findLayerSelections(view);

            for(Layer layer : layers) {
                LayerSelection laySel = (LayerSelection) layer;
                laySel.selectPoint(event.getX(), event.getY());
            }
            view.repaint();
        }
    }

    private class SelectLayerRectangleTool extends SelectRectangleTool {

        @Override
        public void drag(MouseEvent event) {
            super.drag(event);
            int width = event.getX() - referencePoint.x;
            int height = event.getY() - referencePoint.y;
            int x = referencePoint.x;
            int y = referencePoint.y;
            if (width < 0) {
                width *= -1;
                x -= width;
            }
            if (height < 0) {
                height *= -1;
                y -= height;
            }

            final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
            final List<Layer> layers = findLayerSelections(view);

            for(Layer layer : layers) {
                LayerSelection laySel = (LayerSelection) layer;
                laySel.selectRectangle(new Rectangle(x, y, width, height));
            }
        }

        @Override
        public void end(MouseEvent event) {
            super.end(event);
            final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();
            view.repaint();
        }
    }

    private static List<Layer> findLayerSelections(final ProductSceneView view) {
        return LayerUtils.getChildLayers(view.getRootLayer(), LayerUtils.SearchMode.DEEP, new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layer instanceof LayerSelection;
            }
        });
    }
}
