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
class MoveLayerLeftAction extends AbstractAction {

    private final AppContext appContext;

    MoveLayerLeftAction(AppContext appContext) {
        super("Move Layer Left", UIUtils.loadImageIcon("icons/Left24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        final Layer selectedLayer = appContext.getSelectedProductSceneView().getSelectedLayer();
        Layer rootLayer = appContext.getSelectedProductSceneView().getRootLayer();
        if (selectedLayer != null && rootLayer != selectedLayer) {
            moveLeft(selectedLayer);
        }
    }

    void moveLeft(Layer layer) {
        if (canMove(layer)) {
            Layer parentLayer = layer.getParent();
            final Layer parentsParentLayer = parentLayer.getParent();
            parentLayer.getChildren().remove(layer);
            final int parentIndex = parentsParentLayer.getChildIndex(parentLayer.getId());
            if (parentIndex < parentsParentLayer.getChildren().size() - 1) {
                parentsParentLayer.getChildren().add(parentIndex + 1, layer);
            } else {
                parentsParentLayer.getChildren().add(layer);
            }
        }
    }

    public boolean canMove(Layer layer) {
        Layer parentLayer = layer.getParent();
        final Layer parentsParentLayer = parentLayer.getParent();
        return parentsParentLayer != null;
    }

}
