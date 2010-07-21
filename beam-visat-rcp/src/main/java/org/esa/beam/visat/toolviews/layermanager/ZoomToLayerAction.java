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

package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
class ZoomToLayerAction extends AbstractAction {

    private final AppContext appContext;

    ZoomToLayerAction(AppContext appContext) {
        super("Zoom to Layer", UIUtils.loadImageIcon("icons/ZoomTo24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        final ProductSceneView sceneView = appContext.getSelectedProductSceneView();
        final Layer selectedLayer = sceneView.getSelectedLayer();
        Rectangle2D modelBounds = selectedLayer.getModelBounds();
        if (modelBounds != null) {
            final Viewport viewport = sceneView.getLayerCanvas().getViewport();
            final AffineTransform m2vTransform = viewport.getModelToViewTransform();
            final AffineTransform v2mTransform = viewport.getViewToModelTransform();
            final Rectangle2D viewBounds = m2vTransform.createTransformedShape(modelBounds).getBounds2D();
            viewBounds.setFrameFromDiagonal(viewBounds.getMinX() - 10, viewBounds.getMinY() - 10,
                                            viewBounds.getMaxX() + 10, viewBounds.getMaxY() + 10);
            final Shape transformedModelBounds = v2mTransform.createTransformedShape(viewBounds);
            sceneView.zoom(transformedModelBounds.getBounds2D());
        }
    }

}