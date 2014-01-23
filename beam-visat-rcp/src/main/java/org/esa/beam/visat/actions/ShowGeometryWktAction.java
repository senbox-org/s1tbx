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

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.visat.VisatApp;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

/**
 * An action that allows users to copy WKT from selected Geometries.
 *
 * @since BEAM 5
 * @author Norman
 */
public class ShowGeometryWktAction extends AbstractVisatAction implements SelectionChangeListener {

    private static final String DLG_TITLE = "Geometry as WKT";

    private SelectionManager selectionManager;

    /**
     * Invoked when a command action is performed.
     *
     * @param event the command event
     */
    @Override
    public void actionPerformed(CommandEvent event) {
        exportToWkt();
    }

    /**
     * Called when a command should update its state.
     * <p/>
     * <p> This method can contain some code which analyzes the underlying element and makes a decision whether
     * this item or group should be made visible/invisible or enabled/disabled etc.
     *
     * @param event the command event
     */
    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(hasSelectedFeatureFigure());
    }

    private boolean hasSelectedFeatureFigure() {
        return getSelectedFeatureFigure() != null;
    }

    private SimpleFeatureFigure getSelectedFeatureFigure() {
        final SelectionContext selectionContext = getSelectionManager().getSelectionContext();
        if (selectionContext == null) {
            return null;
        }
        final Selection selection = selectionContext.getSelection();
        if (selection.isEmpty()) {
            return null;
        }
        final Object selectedValue = selection.getSelectedValue();
        if (!(selectedValue instanceof SimpleFeatureFigure)) {
            return null;
        }
        return (SimpleFeatureFigure) selectedValue;
    }

    private void exportToWkt() {

        SimpleFeatureFigure selectedFeatureFigure = getSelectedFeatureFigure();
        if (selectedFeatureFigure == null) {
            VisatApp.getApp().showInfoDialog(DLG_TITLE, "Please select a geometry.", null);
            return;
        }
        SimpleFeature simpleFeature = selectedFeatureFigure.getSimpleFeature();
        CoordinateReferenceSystem sourceCrs = simpleFeature.getDefaultGeometryProperty().getDescriptor().getCoordinateReferenceSystem();
        CoordinateReferenceSystem targetCrs = DefaultGeographicCRS.WGS84;

        Geometry sourceGeom = selectedFeatureFigure.getGeometry();
        Geometry targetGeom;
        try {
            targetGeom = transformGeometry(sourceGeom, sourceCrs, targetCrs);
        } catch (Exception e) {
            VisatApp.getApp().showWarningDialog(DLG_TITLE, "Failed to transform geometry to " + targetCrs.getName() + ".\n" +
                    "Using " + sourceCrs.getName() + " instead.");
            targetGeom = sourceGeom;
            targetCrs = sourceCrs;
        }

        WKTWriter wktWriter = new WKTWriter();
        wktWriter.setFormatted(true);
        wktWriter.setMaxCoordinatesPerLine(2);
        wktWriter.setTab(3);
        String wkt = wktWriter.writeFormatted(targetGeom);

        JTextArea textArea = new JTextArea(16, 32);
        textArea.setEditable(false);
        textArea.setText(wkt);
        textArea.selectAll();

        JPanel contentPanel = new JPanel(new BorderLayout(4, 4));
        contentPanel.add(new JLabel("Geometry Well-Known-Text (WKT):"), BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        contentPanel.add(new JLabel("Geometry CRS: " + targetCrs.getName().toString()), BorderLayout.SOUTH);

        ModalDialog modalDialog = new ModalDialog(VisatApp.getApp().getApplicationWindow(), DLG_TITLE, ModalDialog.ID_OK, null);
        modalDialog.setContent(contentPanel);
        modalDialog.center();
        modalDialog.show();
    }

    private Geometry transformGeometry(Geometry sourceGeom, CoordinateReferenceSystem sourceCrs, CoordinateReferenceSystem targetCrs) throws FactoryException, TransformException {
        MathTransform mt = CRS.findMathTransform(sourceCrs, targetCrs, true);
        GeometryCoordinateSequenceTransformer gcst = new GeometryCoordinateSequenceTransformer();
        gcst.setMathTransform(mt);
        return gcst.transform(sourceGeom);
    }

    private SelectionManager getSelectionManager() {
        if (selectionManager == null) {
            selectionManager = getAppContext().getApplicationPage().getSelectionManager();
            selectionManager.addSelectionChangeListener(this);
        }
        return selectionManager;
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Implementation of the SelectionChangeListener interface

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        updateState();
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
        updateState();
    }

    //
    /////////////////////////////////////////////////////////////////////////////////
}