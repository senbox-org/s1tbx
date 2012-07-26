package org.esa.beam.pixex.visat;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.csv.dataio.CsvFile;
import org.esa.beam.csv.dataio.CsvSource;
import org.esa.beam.csv.dataio.CsvSourceParser;
import org.esa.beam.framework.datamodel.GenericPlacemarkDescriptor;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class AddCsvFileAction extends AbstractAction {

    private final JPanel parent;
    private final CoordinateTableModel tableModel;

    public AddCsvFileAction(CoordinateTableModel tableModel, JPanel parent) {
        super("Add measurements from CSV file...");
        this.parent = parent;
        this.tableModel = tableModel;
    }

    //todo clean up

    @Override
    public void actionPerformed(ActionEvent e) {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setFileFilter(new BeamFileFilter("CSV", ".csv", "CSV files"));

//        fileChooser.setCurrentDirectory(new File(lastDir));
        int answer = fileChooser.showDialog(parent, "Select");
        if (answer == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
//            preferences.setPropertyString(LAST_OPEN_PLACEMARK_DIR, selectedFile.getParent());
            CsvSourceParser csvSourceParser = null;
            try {
                csvSourceParser = CsvFile.createCsvSourceParser(selectedFile.getAbsolutePath());
                final CsvSource csvSource = csvSourceParser.parseMetadata();
                csvSourceParser.parseRecords(0, csvSource.getRecordCount());

                final SimpleFeatureType featureType = csvSource.getFeatureType();
                final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
                builder.init(featureType);
                final SimpleFeatureType pointFeatureType = Placemark.createPointFeatureType(
                        featureType.getName().getLocalPart());
                for (AttributeDescriptor attributeDescriptor : pointFeatureType.getAttributeDescriptors()) {
                    builder.add(attributeDescriptor);
                }
                final SimpleFeatureType myFeatureType = builder.buildFeatureType();

                final GenericPlacemarkDescriptor descriptor = new GenericPlacemarkDescriptor(myFeatureType);
                final SimpleFeature[] simpleFeatures = csvSource.getSimpleFeatures();
                final SimpleFeatureBuilder builder1 = new SimpleFeatureBuilder(myFeatureType);
                for (SimpleFeature simpleFeature : simpleFeatures) {
                    builder1.init(simpleFeature);
                    final SimpleFeature newFeature = builder1.buildFeature(simpleFeature.getID());
                    builder1.reset();

                    final Placemark placemark = descriptor.createPlacemark(newFeature);
                    final Geometry defaultGeometry = (Geometry) newFeature.getDefaultGeometry();
                    final Point centroid = defaultGeometry.getCentroid();
                    placemark.setGeoPos(new GeoPos((float) centroid.getY(), (float) centroid.getX()));
                    tableModel.addPlacemark(placemark);
                }

            } catch (IOException ioe) {
                ioe.printStackTrace();
//                appContext.handleError(String.format("Error occurred while reading file: %s", selectedFile), ioe);
            } finally {
                if (csvSourceParser != null) {
                    csvSourceParser.close();
                }
            }
        }

    }
}
