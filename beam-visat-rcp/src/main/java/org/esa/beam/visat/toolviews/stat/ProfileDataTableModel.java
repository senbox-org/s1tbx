package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.TransectProfileData;
import org.esa.beam.framework.ui.io.CsvEncoder;
import org.esa.beam.framework.ui.io.TableModelCsvEncoder;
import org.opengis.feature.simple.SimpleFeature;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

/**
 * @author Norman Fomferra
 */
public class ProfileDataTableModel extends AbstractTableModel implements CsvEncoder {
    private final TransectProfileData profileData;
    private final String[] columnNames;
    private final Class[] columnClasses;
    private final int[] pointDataIndexes;
    private final int dataFieldIndex;
    private final SimpleFeature[] features;
    private final boolean computeInBetweenPoints;

    public ProfileDataTableModel(String sampleName,
                                 TransectProfileData profileData,
                                 ProfilePlotPanel.DataSourceConfig dataSourceConfig) {

        this.profileData = profileData;

        pointDataIndexes = new int[profileData.getNumPixels()];
        Arrays.fill(pointDataIndexes, -1);
        int[] shapeVertexIndexes = profileData.getShapeVertexIndexes();
        for (int i = 0; i < shapeVertexIndexes.length; i++) {
            int shapeVertexIndex = shapeVertexIndexes[i];
            pointDataIndexes[shapeVertexIndex] = i;
        }

        computeInBetweenPoints = dataSourceConfig.computeInBetweenPoints;

        final String corrDataName;
        final Class corrDataClass;
        if (dataSourceConfig.pointDataSource != null && dataSourceConfig.dataField != null) {
            corrDataName = dataSourceConfig.dataField.getLocalName();
            corrDataClass = dataSourceConfig.dataField.getType().getBinding();
            features = dataSourceConfig.pointDataSource.getFeatureCollection().toArray(new SimpleFeature[0]);
            dataFieldIndex = dataSourceConfig.pointDataSource.getFeatureType().indexOf(corrDataName);
        } else {
            corrDataName = "";
            corrDataClass = Object.class;
            features = null;
            dataFieldIndex = -1;
        }

        columnNames = new String[]{
                "pixel_no",
                "pixel_x",
                "pixel_y",
                "latitude",
                "longitude",
                sampleName + "_mean",
                sampleName + "_sigma",
                corrDataName
        };

        columnClasses = new Class[]{
                Integer.class,
                Integer.class,
                Integer.class,
                Float.class,
                Float.class,
                Float.class,
                Float.class,
                corrDataClass
        };
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columnClasses[column];
    }

    @Override
    public int getRowCount() {
        return computeInBetweenPoints ? profileData.getNumPixels() : profileData.getNumShapeVertices();
    }

    @Override
    public Object getValueAt(int row, int column) {

        int pixelIndex = computeInBetweenPoints ? row : profileData.getShapeVertexIndexes()[row];

        if (column == 0) {
            return pixelIndex + 1;
        } else if (column == 1) {
            return profileData.getPixelPositions()[pixelIndex].getX();
        } else if (column == 2) {
            return profileData.getPixelPositions()[pixelIndex].getY();
        } else if (column == 3) {
            GeoPos[] geoPositions = profileData.getGeoPositions();
            return geoPositions.length > 0 ? geoPositions[pixelIndex].getLat() : null;
        } else if (column == 4) {
            GeoPos[] geoPositions = profileData.getGeoPositions();
            return geoPositions.length > 0 ? geoPositions[pixelIndex].getLon() : null;
        } else if (column == 5) {
            return profileData.getSampleValues()[pixelIndex];
        } else if (column == 6) {
            return profileData.getSampleSigmas()[pixelIndex];
        } else if (column == 7) {
            if (dataFieldIndex == -1) {
                return null;
            }
            int pointDataIndex = pointDataIndexes[pixelIndex];
            if (pointDataIndex == -1) {
                return null;
            }
            return features[pointDataIndex].getAttribute(dataFieldIndex);
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public String toCsv() {
        StringWriter sw = new StringWriter();
        try {
            encodeCsv(sw);
            sw.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return sw.toString();
    }

    @Override
    public void encodeCsv(Writer writer) throws IOException {
        new TableModelCsvEncoder(this).encodeCsv(writer);
    }
}
