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

import org.esa.beam.util.io.CsvFile;
import org.esa.beam.util.io.CsvSource;
import org.esa.beam.util.io.CsvSourceParser;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;


/**
 * Experimental action that lets a user load CSV files that contain points given by a geo-point, and which contains
 * arbitrary EO data.
 *
 * @author Thomas Storm
 * @since BEAM 4.10
 */
public class ImportPointAction extends ExecCommand {

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

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        try {
            featureCollection = readPoints(file, product.getGeoCoding());
        } catch (IOException e) {
            visatApp.showErrorDialog(TITLE, "Failed to load csv file:\n" + e.getMessage());
            return;
        }

        if (featureCollection.isEmpty()) {
            visatApp.showErrorDialog(TITLE, "No records found.");
            return;
        }

        String name = FileUtils.getFilenameWithoutExtension(file);
        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection);
        vectorDataNode.setDefaultStyleCss("symbol: square; stroke:#ffaaff; stroke-opacity:1.0; stroke-width:0.0");
        product.getVectorDataGroup().add(vectorDataNode);
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null
                           && VisatApp.getApp().getSelectedProduct().getGeoCoding() != null);
    }

    private static FeatureCollection<SimpleFeatureType, SimpleFeature> readPoints(File file, GeoCoding geoCoding) throws IOException {
        CsvSourceParser csvFile = null;
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = null;
        try{
            csvFile = CsvFile.createCsvSourceParser(file, geoCoding.getImageCRS());
            final CsvSource csvSource = csvFile.parseMetadata();
            csvFile.parseRecords(0, -1);
            featureCollection = new ListFeatureCollection(csvSource.getFeatureType());
            featureCollection.addAll(Arrays.asList(csvSource.getSimpleFeatures()));
        } catch (CsvSourceParser.ParseException e) {
            throw new IOException(e);
        } finally {
            if (csvFile != null) {
                csvFile.close();
            }
        }
        return featureCollection;
    }
}
