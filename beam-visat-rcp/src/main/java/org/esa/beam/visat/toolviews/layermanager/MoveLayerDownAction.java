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
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

/**
 * @author Marco Peters
 * @version $Revision: $ $Date: $
 * @since BEAM 4.6
 */
class MoveLayerDownAction extends AbstractAction {

    private final AppContext appContext;

    MoveLayerDownAction(AppContext appContext) {
        super("Move Layer Down", UIUtils.loadImageIcon("icons/Down24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        final Layer selectedLayer = appContext.getSelectedProductSceneView().getSelectedLayer();
        Layer rootLayer = appContext.getSelectedProductSceneView().getRootLayer();
        if (selectedLayer != null && rootLayer != selectedLayer) {
            moveDown(selectedLayer);
        }
    }

    void moveDown(Layer layer) {
        if (canMove(layer)) {
            final Layer parentLayer = layer.getParent();
            final int layerIndex = parentLayer.getChildIndex(layer.getId());
            parentLayer.getChildren().remove(layer);
            parentLayer.getChildren().add(layerIndex + 1, layer);
        }
    }

    public boolean canMove(Layer layer) {
        final Layer parentLayer = layer.getParent();
        final int layerIndex = parentLayer.getChildIndex(layer.getId());

        final int lastIndex = parentLayer.getChildren().size() - 1;
        return layerIndex != lastIndex;
    }
}