/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.geometry.VectorDataNodeIO;
import org.esa.beam.dataio.geometry.VectorDataNodeReader2;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.visat.VisatApp;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;


/**
 * Experimental action that lets a user load CSV files that contain points given by a geo-point, and which contains
 * arbitrary EO data.
 *
 * @author BEAM Team
 * @since BEAM 4.10
 */
public class ImportPointDataAction extends ExecCommand {

    public static final String TITLE = "Open CSV File";
    public static final String PROPERTY_KEY_LAST_DIR = "importCsv.lastDir";

    @Override
    public void actionPerformed(final CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();

        File file = visatApp.showFileOpenDialog(TITLE, false, null, PROPERTY_KEY_LAST_DIR);
        if (file == null) {
            return;
        }
        visatApp.getPreferences().setPropertyString(PROPERTY_KEY_LAST_DIR, file.getAbsolutePath());

        Product product = visatApp.getSelectedProduct();

        // todo - always expect WGS-84 and convert all geometry coordinates into Model CRS of product. (nf,se 2012-03-29)

        final VectorDataNode vectorDataNode;
        CoordinateReferenceSystem modelCrs;
        try {
            modelCrs = product.getGeoCoding() != null ? ImageManager.getModelCrs(product.getGeoCoding()) :
                       ImageManager.DEFAULT_IMAGE_CRS;
            vectorDataNode = VectorDataNodeReader2.read(file.getName(), new FileReader(file), product, null, null, null, ProgressMonitor.NULL);
        } catch (IOException e) {
            visatApp.showErrorDialog(TITLE, "Failed to load csv file:\n" + e.getMessage());
            return;
        }

        TypeDialog typeDialog = new TypeDialog(visatApp.getApplicationWindow(), vectorDataNode.getFeatureType());
        if (typeDialog.show() != ModalDialog.ID_OK) {
            return;
        }

        int featureCount = vectorDataNode.getFeatureCollection().size();
        boolean individualShapes = false;
        String attributeName = null;
        if (featureCount > 1) {
            String text = "<html>" +
                          "The data contains <b>" +
                          vectorDataNode.getFeatureCollection().size() + " </b>features.<br>" +
                          "Shall they be imported separately?<br>" +
                          "<br>" +
                          "If you select <b>Yes</b>, the features can be used as individual masks<br>" +
                          "and they will be displayed on individual layers.</i>";
            SeparateGeometriesDialog separateGeometriesDialog = new SeparateGeometriesDialog(visatApp.getMainFrame(), vectorDataNode, getHelpId(), text);

            int response = separateGeometriesDialog.show();
            if (response == ModalDialog.ID_CANCEL) {
                return;
            }

            individualShapes = response == ModalDialog.ID_YES;
            attributeName = separateGeometriesDialog.getSelectedAttributeName();

        }
        VectorDataNode[] vectorDataNodes = VectorDataNodeIO.getVectorDataNodes(vectorDataNode, individualShapes, attributeName);
        for (VectorDataNode vectorDataNode1 : vectorDataNodes) {
            product.getVectorDataGroup().add(vectorDataNode1);
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null
                   && VisatApp.getApp().getSelectedProduct().getGeoCoding() != null);
    }
}
