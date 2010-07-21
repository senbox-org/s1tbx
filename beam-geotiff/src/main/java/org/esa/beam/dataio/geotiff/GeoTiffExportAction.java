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

package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.ProductExportAction;

import javax.swing.JOptionPane;
import java.io.File;

/**
 * @version $Revision: $ $Date: $
 * @since BEAM 4.5
 */
public class GeoTiffExportAction extends ProductExportAction {

    @Override
    protected File promptForFile(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (!(geoCoding instanceof MapGeoCoding) && !(geoCoding instanceof CrsGeoCoding)) {
            final String message = String.format("The product %s is not reprojected to a map.\n" +
                                                 "Un-projected raster data is not well supported by other GIS software.\n" +
                                                 "\n" +
                                                 "Do you want to export the product without a reprojection?",
                                                 product.getName());
            final int answer = JOptionPane.showConfirmDialog(VisatApp.getApp().getMainFrame(), message, getText(),
                                                             JOptionPane.YES_NO_OPTION,
                                                             JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return super.promptForFile(product);
    }
}
