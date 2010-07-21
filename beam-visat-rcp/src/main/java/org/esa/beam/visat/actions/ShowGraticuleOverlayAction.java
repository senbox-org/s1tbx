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

import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.VisatApp;

public class ShowGraticuleOverlayAction extends AbstractShowOverlayAction {

    @Override
    public void actionPerformed(CommandEvent event) {
        final ProductSceneView view = VisatApp.getApp().getSelectedProductSceneView();

        if (view != null) {
            if (ProductUtils.canGetPixelPos(view.getRaster())) {
                view.setGraticuleOverlayEnabled(isSelected());
            }
        }
    }

    @Override
    protected void updateEnableState(ProductSceneView view) {
        setEnabled(ProductUtils.canGetPixelPos(view.getRaster()));
    }

    @Override
    protected void updateSelectState(ProductSceneView view) {
        setSelected(view.isGraticuleOverlayEnabled());
    }
}
