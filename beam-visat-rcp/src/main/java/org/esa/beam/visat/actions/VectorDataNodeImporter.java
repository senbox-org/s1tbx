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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.vividsolutions.jts.geom.Polygonal;
import org.esa.beam.dataio.geometry.VectorDataNodeIO;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.Debug;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryDescriptor;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

// todo - test with shapefile that has no CRS (nf, 2012-04-05)

class VectorDataNodeImporter {

    private final String dialogTitle;
    private final String shapeIoDirPreferencesKey;
    private String helpId;
    private BeamFileFilter filter;
    private final VectorDataNodeReader reader;

    VectorDataNodeImporter(String helpId, BeamFileFilter filter, VectorDataNodeReader reader, String dialogTitle, String shapeIoDirPreferencesKey) {
        this.helpId = helpId;
        this.filter = filter;
        this.reader = reader;
        this.dialogTitle = dialogTitle;
        this.shapeIoDirPreferencesKey = shapeIoDirPreferencesKey;
    }

    void importGeometry(final VisatApp visatApp) {
        final PropertyMap propertyMap = visatApp.getPreferences();
        final BeamFileChooser fileChooser = new BeamFileChooser();
        HelpSys.enableHelpKey(fileChooser, helpId);
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setFileFilter(filter);
//        fileChooser.addChoosableFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setCurrentDirectory(getIODir(propertyMap));
        final int result = fileChooser.showOpenDialog(visatApp.getMainFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(propertyMap, file.getAbsoluteFile().getParentFile());
                importGeometry(visatApp, file);
            }
        }
    }

    private void importGeometry(final VisatApp visatApp, final File file) {
        final Product product = VisatApp.getApp().getSelectedProduct();
        if (product == null) {
            return;
        }

        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null || !geoCoding.canGetPixelPos()) {
            visatApp.showErrorDialog(dialogTitle, "Failed to import vector data.\n"
                    +
                    "Current geo-coding cannot convert from geographic to pixel coordinates."); /* I18N */
            return;
        }

        VectorDataNode vectorDataNode;
        try {
            vectorDataNode = readGeometry(visatApp, file, product);
        } catch (Exception e) {
            visatApp.showErrorDialog(dialogTitle, "Failed to import vector data.\n" + "An I/O Error occurred:\n"
                    + e.getMessage()); /* I18N */
            Debug.trace(e);
            return;
        }

        if (vectorDataNode.getFeatureCollection().isEmpty()) {
            visatApp.showErrorDialog(dialogTitle, "The vector data was loaded successfully,\n"
                    + "but no part is located within the scene boundaries."); /* I18N */
            return;
        }

        boolean individualShapes = false;
        String attributeName = null;
        GeometryDescriptor geometryDescriptor = vectorDataNode.getFeatureType().getGeometryDescriptor();
        int featureCount = vectorDataNode.getFeatureCollection().size();
        if (featureCount > 1
                && geometryDescriptor != null
                && Polygonal.class.isAssignableFrom(geometryDescriptor.getType().getBinding())) {

            String text = "<html>" +
                    "The vector data set contains <b>" +
                    featureCount + "</b> polygonal shapes.<br>" +
                    "Shall they be imported separately?<br>" +
                    "<br>" +
                    "If you select <b>Yes</b>, the polygons can be used as individual masks<br>" +
                    "and they will be displayed on individual layers.</i>";
            SeparateGeometriesDialog dialog = new SeparateGeometriesDialog(visatApp.getMainFrame(), vectorDataNode, helpId,
                    text);

            int response = dialog.show();
            if (response == ModalDialog.ID_CANCEL) {
                return;
            }

            individualShapes = response == ModalDialog.ID_YES;
            attributeName = dialog.getSelectedAttributeName();
        }

        VectorDataNode[] vectorDataNodes = VectorDataNodeIO.getVectorDataNodes(vectorDataNode, individualShapes, attributeName);
        for (VectorDataNode vectorDataNode1 : vectorDataNodes) {
            product.getVectorDataGroup().add(vectorDataNode1);
        }

        setLayersVisible(vectorDataNodes);
    }

    private void setLayersVisible(VectorDataNode[] vectorDataNodes) {
        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            sceneView.setLayersVisible(vectorDataNodes);
        }
    }

    static String findUniqueVectorDataNodeName(String suggestedName, ProductNodeGroup<VectorDataNode> vectorDataGroup) {
        String name = suggestedName;
        int index = 1;
        while (vectorDataGroup.contains(name)) {
            name = suggestedName + "_" + index;
            index++;
        }
        return name;
    }

    private File getIODir(final PropertyMap propertyMap) {
        final File dir = SystemUtils.getUserHomeDir();
        return new File(propertyMap.getPropertyString(shapeIoDirPreferencesKey, dir.getPath()));
    }

    public String getDialogTitle() {
        return dialogTitle;
    }

    interface VectorDataNodeReader {

        VectorDataNode readVectorDataNode(VisatApp visatApp, File file, Product product, String helpId, ProgressMonitor pm) throws IOException;
    }

    private VectorDataNode readGeometry(final VisatApp visatApp,
                                        final File file,
                                        final Product product)
            throws IOException, ExecutionException, InterruptedException {

        ProgressMonitorSwingWorker<VectorDataNode, Object> worker = new ProgressMonitorSwingWorker<VectorDataNode, Object>(visatApp.getMainFrame(), "Loading vector data") {
            @Override
            protected VectorDataNode doInBackground(ProgressMonitor pm) throws Exception {
                return reader.readVectorDataNode(visatApp, file, product, helpId, pm);
            }

            @Override
            protected void done() {
                super.done();
            }
        };

        worker.executeWithBlocking();
        return worker.get();
    }

    private void setIODir(final PropertyMap propertyMap, final File dir) {
        if (dir != null) {
            propertyMap.setPropertyString(shapeIoDirPreferencesKey, dir.getPath());
        }
    }

}
