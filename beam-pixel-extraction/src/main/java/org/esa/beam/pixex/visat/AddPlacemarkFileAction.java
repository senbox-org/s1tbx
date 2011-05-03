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

package org.esa.beam.pixex.visat;

import org.esa.beam.dataio.placemark.PlacemarkIO;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

class AddPlacemarkFileAction extends AbstractAction {

    private static final String LAST_OPEN_PLACEMARK_DIR = "beam.pixex.lastOpenPlacemarkDir";

    private final CoordinateTableModel tableModel;
    private final AppContext appContext;
    private final JComponent parentComponent;

    AddPlacemarkFileAction(AppContext appContext, CoordinateTableModel tableModel, JPanel parentComponent) {
        super("Add coordinates from file...");
        this.tableModel = tableModel;
        this.appContext = appContext;
        this.parentComponent = parentComponent;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PropertyMap preferences = appContext.getPreferences();
        String lastDir = preferences.getPropertyString(LAST_OPEN_PLACEMARK_DIR,
                                                       SystemUtils.getUserHomeDir().getPath());
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(PlacemarkIO.createPlacemarkFileFilter());
        fileChooser.setFileFilter(PlacemarkIO.createTextFileFilter());

        fileChooser.setCurrentDirectory(new File(lastDir));
        int answer = fileChooser.showDialog(parentComponent, "Select");
        if (answer == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            preferences.setPropertyString(LAST_OPEN_PLACEMARK_DIR, selectedFile.getParent());
            FileReader reader = null;
            try {
                reader = new FileReader(selectedFile);
                final List<Placemark> placemarks = PlacemarkIO.readPlacemarks(reader, null, PinDescriptor.INSTANCE);
                for (Placemark placemark : placemarks) {
                    tableModel.addPlacemark(placemark);
                }
            } catch (IOException ioe) {
                appContext.handleError(String.format("Error occurred while reading file: %s", selectedFile), ioe);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

    }
}
