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

package org.esa.beam.visat.toolviews.imageinfo;

import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Component;

class EmptyImageInfoForm implements ColorManipulationChildForm {
    public static final ColorManipulationChildForm INSTANCE = new EmptyImageInfoForm();

    private EmptyImageInfoForm() {
    }

    public void handleFormShown(ProductSceneView productSceneView) {
    }

    public void handleFormHidden(ProductSceneView productSceneView) {
    }

    public void updateFormModel(ProductSceneView productSceneView) {
    }

    public void resetFormModel(ProductSceneView productSceneView) {
    }

    public void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster) {
    }

    public AbstractButton[] getToolButtons() {
        return new AbstractButton[0];
    }

    public Component getContentPanel() {
        return new JLabel("No image view selected.");
    }

    public RasterDataNode[] getRasters() {
        return new RasterDataNode[0];
    }

    public MoreOptionsForm getMoreOptionsForm() {
        return null;
    }
}
