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

package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import org.esa.beam.framework.ui.FileHistory;
import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.beam.framework.ui.layer.LayerSourcePageContext;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.ShapefileUtils;
import org.esa.beam.visat.toolviews.layermanager.layersrc.FilePathListCellRenderer;
import org.esa.beam.visat.toolviews.layermanager.layersrc.HistoryComboBoxModel;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;

class ShapefileAssistantPage1 extends AbstractLayerSourceAssistantPage {

    private static final String PROPERTY_LAST_FILE_PREFIX = "ShapefileAssistant.Shapefile.history";
    private static final String PROPERTY_LAST_DIR = "ShapefileAssistant.Shapefile.lastDir";

    private HistoryComboBoxModel fileHistoryModel;


    ShapefileAssistantPage1() {
        super("Select ESRI Shapefile");
    }

    @Override
    public Component createPageComponent() {
        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());

        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 2;
        panel.add(new JLabel("Path to ESRI Shapefile (*.shp):"), gbc);

        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 1;
        final LayerSourcePageContext context = getContext();
        final PropertyMap preferences = context.getAppContext().getPreferences();
        final FileHistory fileHistory = new FileHistory(5, PROPERTY_LAST_FILE_PREFIX);
        fileHistory.initBy(preferences);
        fileHistoryModel = new HistoryComboBoxModel(fileHistory);
        JComboBox shapefileBox = new JComboBox(fileHistoryModel);
        shapefileBox.setRenderer(new FilePathListCellRenderer(80));
        shapefileBox.setEditable(true);
        shapefileBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                context.updateState();
            }
        });
        panel.add(shapefileBox, gbc);

        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        JButton button = new JButton("...");
        button.addActionListener(new ShpaeFilechooserActionListener());
        panel.add(button, gbc);

        return panel;
    }

    @Override
    public boolean validatePage() {
        if (fileHistoryModel != null) {
            String path = (String) fileHistoryModel.getSelectedItem();
            return path != null && !path.trim().isEmpty();
        }
        return false;
    }

    @Override
    public boolean hasNextPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        final LayerSourcePageContext context = getContext();
        fileHistoryModel.getHistory().copyInto(context.getAppContext().getPreferences());
        String path = (String) fileHistoryModel.getSelectedItem();
        if (path != null && !path.trim().isEmpty()) {
            try {
                final String oldPath = (String) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FILE_PATH);
                if (!path.equals(oldPath)) {
                    context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FILE_PATH, path);
                    final URL fileUrl = new File(path).toURI().toURL();
                    final FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = ShapefileUtils.getFeatureSource(fileUrl);
                    context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FEATURE_COLLECTION, featureSource.getFeatures());
                    // clear other properties they are not valid anymore
                    context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_SELECTED_STYLE, null);
                    context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_STYLES, null);
                    context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FEATURE_COLLECTION_CRS, null);
                }
                FeatureCollection fc = (FeatureCollection) context.getPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FEATURE_COLLECTION);
                final CoordinateReferenceSystem featureCrs = fc.getSchema().getCoordinateReferenceSystem();
                if (featureCrs == null) {
                    return new ShapefileAssistantPage2();
                } else {
                    context.setPropertyValue(ShapefileLayerSource.PROPERTY_NAME_FEATURE_COLLECTION_CRS, featureCrs);
                    return new ShapefileAssistantPage3();
                }
            } catch (Exception e) {
                e.printStackTrace();
                context.showErrorDialog("Failed to load ESRI shapefile:\n" + e.getMessage());
            }
        }

        return null;
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    private class ShpaeFilechooserActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            final FileNameExtensionFilter shapefileFilter = new FileNameExtensionFilter("ESRI Shapefile", "shp");
            fileChooser.addChoosableFileFilter(shapefileFilter);
            fileChooser.setFileFilter(shapefileFilter);
            File lastDir = getLastDirectory();
            fileChooser.setCurrentDirectory(lastDir);
            LayerSourcePageContext pageContext = getContext();
            fileChooser.showOpenDialog(pageContext.getWindow());
            if (fileChooser.getSelectedFile() != null) {
                String filePath = fileChooser.getSelectedFile().getPath();
                fileHistoryModel.setSelectedItem(filePath);
                PropertyMap preferences = pageContext.getAppContext().getPreferences();
                preferences.setPropertyString(PROPERTY_LAST_DIR, fileChooser.getCurrentDirectory().getAbsolutePath());
                pageContext.updateState();
            }
        }

        private File getLastDirectory() {
            PropertyMap preferences = getContext().getAppContext().getPreferences();
            String dirPath = preferences.getPropertyString(PROPERTY_LAST_DIR, System.getProperty("user.home"));
            File lastDir = new File(dirPath);
            if (!lastDir.isDirectory()) {
                lastDir = new File(System.getProperty("user.home"));
            }
            return lastDir;
        }
    }

}
