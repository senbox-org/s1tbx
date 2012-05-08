package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.TransectProfileData;
import org.esa.beam.framework.ui.io.CsvEncoder;
import org.esa.beam.framework.ui.io.TableModelCsvEncoder;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
class ProfileDataTableModel extends AbstractTableModel implements CsvEncoder {
    private final static String REF_SUFFIX = "_ref";

    private final TransectProfileData profileData;
    private final List<String> columnNames;
    private final Map<Integer, Integer> propertyIndices;
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
        if (dataSourceConfig.pointDataSource != null && dataSourceConfig.dataField != null) {
            corrDataName = dataSourceConfig.dataField.getLocalName();
            features = dataSourceConfig.pointDataSource.getFeatureCollection().toArray(new SimpleFeature[0]);
            dataFieldIndex = dataSourceConfig.pointDataSource.getFeatureType().indexOf(corrDataName);
        } else {
            corrDataName = "";
            features = null;
            dataFieldIndex = -1;
        }

        columnNames = new ArrayList<String>();
        columnNames.add("pixel_no");
        columnNames.add("pixel_x");
        columnNames.add("pixel_y");
        columnNames.add("latitude");
        columnNames.add("longitude");
        columnNames.add(sampleName + "_mean");
        columnNames.add(sampleName + "_sigma");
        columnNames.add(corrDataName.trim().length() == 0 ? "" : corrDataName + REF_SUFFIX);

        propertyIndices = new HashMap<Integer, Integer>();
        if (features != null && features.length > 0) {
            final int colStart = 8;

            int validPropertyCount = 0;
            final Collection<Property> props = features[0].getProperties();
            final Property[] properties = props.toArray(new Property[props.size()]);
            for (int i = 0; i < properties.length; i++) {
                Property property = properties[i];
                final String name = property.getName().toString();
                if (!corrDataName.equals(name)) {
                    columnNames.add(name + REF_SUFFIX);
                    propertyIndices.put(colStart + validPropertyCount, i);
                    validPropertyCount++;
                }
            }
        }
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column);
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
        } else if (column < getColumnCount()) {
            int pointDataIndex = pointDataIndexes[pixelIndex];
            if (pointDataIndex == -1) {
                return null;
            }
            final Collection<Property> propColl = features[pointDataIndex].getProperties();
            final Property[] properties = propColl.toArray(new Property[propColl.size()]);
            final Integer propertyIndex = propertyIndices.get(column);
            return properties[propertyIndex].getValue();
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
