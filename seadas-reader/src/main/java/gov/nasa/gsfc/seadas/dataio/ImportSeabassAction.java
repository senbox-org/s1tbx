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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;


/**
 * Action that lets a user load a SeaBASS file.
 *
 * @author Don Shea
 * @since SeaDAS 7.0
 * @see <a href="http://seabass.gsfc.nasa.gov/wiki/article.cgi?article=Data_Submission#Data%20Format">SeaBASS File Format Description</a>
 */
public class ImportSeabassAction extends ExecCommand {

    public static final String TITLE = "Open SeaBASS File";

    @Override
    public void actionPerformed(final CommandEvent event) {
        VisatApp visatApp = VisatApp.getApp();

        File file = visatApp.showFileOpenDialog(TITLE, false, null, "importSeabass.lastDir");
        if (file == null) {
            return;
        }

        Product product = visatApp.getSelectedProduct();

        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        try {
            featureCollection = readTrack(file, product.getGeoCoding());
        } catch (IOException e) {
            visatApp.showErrorDialog(TITLE, "Failed to load SeaBASS file:\n" + e.getMessage());
            return;
        }

        if (featureCollection.isEmpty()) {
            visatApp.showErrorDialog(TITLE, "No records found.");
            return;
        }

        String name = file.getName();
        final PlacemarkDescriptor placemarkDescriptor = PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(featureCollection.getSchema());
        placemarkDescriptor.setUserDataOf(featureCollection.getSchema());
        VectorDataNode vectorDataNode = new VectorDataNode(name, featureCollection, placemarkDescriptor);

        product.getVectorDataGroup().add(vectorDataNode);

        final ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            view.setLayersVisible(vectorDataNode);
        }
    }

    @Override
    public void updateState(final CommandEvent event) {
        setEnabled(VisatApp.getApp().getSelectedProduct() != null
                && VisatApp.getApp().getSelectedProduct().getGeoCoding() != null);
    }

    private static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(File file, GeoCoding geoCoding) throws IOException {
        Reader reader = new FileReader(file);
        try {
            return readTrack(reader, geoCoding);
        } finally {
            reader.close();
        }
    }

    static FeatureCollection<SimpleFeatureType, SimpleFeature> readTrack(Reader reader, GeoCoding geoCoding) throws IOException {
        SeabassReader seabassReader = new SeabassReader(reader, geoCoding);
        return seabassReader.createFeatureCollection();
    }


}
