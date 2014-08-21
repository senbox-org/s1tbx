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

import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.AbstractButton;
import java.awt.Component;


interface ColorManipulationChildForm {
    void handleFormShown(FormModel formModel);

    void handleFormHidden(FormModel formModel);

    void updateFormModel(FormModel formModel);

    void resetFormModel(FormModel formModel);

    void handleRasterPropertyChange(ProductNodeEvent event, RasterDataNode raster);

    Component getContentPanel();

    AbstractButton[] getToolButtons();

    MoreOptionsForm getMoreOptionsForm();

    RasterDataNode[] getRasters();
}
