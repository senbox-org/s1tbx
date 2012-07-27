package org.esa.beam.pixex.visat;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.csv.dataio.CsvFile;
import org.esa.beam.csv.dataio.CsvSource;
import org.esa.beam.csv.dataio.CsvSourceParser;
import org.esa.beam.framework.datamodel.GenericPlacemarkDescriptor;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.SystemUtils;
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

class AddCsvFileAction extends AbstractAction {

    private static final String LAST_OPEN_CSV_DIR = "beam.pixex.lastOpenCsvDir";

    private final AppContext appContext;
    private final JPanel parent;
    private final CoordinateTableModel tableModel;

    AddCsvFileAction(AppContext appContext, CoordinateTableModel tableModel, JPanel parent) {
        super("Add measurements from CSV file...");
        this.appContext = appContext;
        this.parent = parent;
        this.tableModel = tableModel;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PropertyMap preferences = appContext.getPreferences();
        final BeamFileChooser fileChooser = getFileChooser(preferences.getPropertyString(LAST_OPEN_CSV_DIR, SystemUtils.getUserHomeDir().getPath()));
        int answer = fileChooser.showDialog(parent, "Select");
        if (answer == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            preferences.setPropertyString(LAST_OPEN_CSV_DIR, selectedFile.getParent());
            CsvSourceParser csvSourceParser = null;
            try {
                csvSourceParser = CsvFile.createCsvSourceParser(selectedFile.getAbsolutePath());
                final CsvSource csvSource = csvSourceParser.parseMetadata();
                csvSourceParser.parseRecords(0, csvSource.getRecordCount());

                final SimpleFeatureType extendedFeatureType = getExtendedFeatureType(csvSource.getFeatureType());

                final GenericPlacemarkDescriptor placemarkDescriptor = new GenericPlacemarkDescriptor(extendedFeatureType);
                final SimpleFeature[] simpleFeatures = csvSource.getSimpleFeatures();
                final SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(extendedFeatureType);
                for (SimpleFeature simpleFeature : simpleFeatures) {
                    final SimpleFeature extendedFeature = getExtendedFeature(simpleFeatureBuilder, simpleFeature);
                    final Placemark placemark = placemarkDescriptor.createPlacemark(extendedFeature);
                    setPlacemarkGeoPos(extendedFeature, placemark);
                    tableModel.addPlacemark(placemark);
                }

            } catch (IOException exception) {
                appContext.handleError(String.format("Error occurred while reading file: %s", selectedFile), exception);
            } finally {
                if (csvSourceParser != null) {
                    csvSourceParser.close();
                }
            }
        }

    }

    private void setPlacemarkGeoPos(SimpleFeature extendedFeature, Placemark placemark) throws IOException {
        final Geometry defaultGeometry = (Geometry) extendedFeature.getDefaultGeometry();
        if (defaultGeometry == null) {
            throw new IOException("Could not read geometry of feature '" + extendedFeature.getID() + "'.");
        }
        final Point centroid = defaultGeometry.getCentroid();
        placemark.setGeoPos(new GeoPos((float) centroid.getY(), (float) centroid.getX()));
    }

    private SimpleFeature getExtendedFeature(SimpleFeatureBuilder simpleFeatureBuilder, SimpleFeature simpleFeature) {
        simpleFeatureBuilder.init(simpleFeature);
        final SimpleFeature extendedFeature = simpleFeatureBuilder.buildFeature(simpleFeature.getID());
        extendedFeature.setDefaultGeometry(simpleFeature.getDefaultGeometry());
        simpleFeatureBuilder.reset();
        return extendedFeature;
    }

    private SimpleFeatureType getExtendedFeatureType(SimpleFeatureType featureType) {
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(featureType);
        final SimpleFeatureType pointFeatureType = Placemark.createPointFeatureType(
                featureType.getName().getLocalPart());
        for (AttributeDescriptor attributeDescriptor : pointFeatureType.getAttributeDescriptors()) {
            builder.add(attributeDescriptor);
        }
        return builder.buildFeatureType();
    }

    private BeamFileChooser getFileChooser(String lastDir) {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setFileFilter(new BeamFileFilter("CSV", ".csv", "CSV files"));
        fileChooser.setCurrentDirectory(new File(lastDir));
        return fileChooser;
    }
}
