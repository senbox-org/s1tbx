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
class RemoveLayerAction extends AbstractAction {

    private final AppContext appContext;


    RemoveLayerAction(AppContext appContext) {
        super("Remove Layer", UIUtils.loadImageIcon("icons/Minus24.gif"));
        this.appContext = appContext;
        putValue(Action.ACTION_COMMAND_KEY, RemoveLayerAction.class.getName());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Layer selectedLayer = appContext.getSelectedProductSceneView().getSelectedLayer();
        if (selectedLayer != null) {
            selectedLayer.getParent().getChildren().remove(selectedLayer);
            selectedLayer.dispose();
        }
    }


}
