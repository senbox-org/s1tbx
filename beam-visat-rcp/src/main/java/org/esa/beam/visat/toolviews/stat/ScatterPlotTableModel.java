package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.ui.io.CsvEncoder;
import org.esa.beam.framework.ui.io.TableModelCsvEncoder;
import org.opengis.feature.Property;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sabine Embacher
 */
public class ScatterPlotTableModel extends AbstractTableModel implements CsvEncoder {

    private final List<String> colNames;
    private final Map<Integer, Integer> propertyIndices;
    private final ScatterPlotPanel.ComputedData[] computedDatas;

    public ScatterPlotTableModel(String rasterName, String correlativDataName, ScatterPlotPanel.ComputedData[] computedDatas) {

        this.computedDatas = computedDatas;
        colNames = new ArrayList<String>();
        colNames.add("pixel_no");
        colNames.add("pixel_x");
        colNames.add("pixel_y");
        colNames.add("latitude");
        colNames.add("longitude");
        colNames.add(rasterName + "_mean");
        colNames.add(rasterName + "_sigma");
        colNames.add(correlativDataName);

        final int colStart = 8;
        propertyIndices = new HashMap<Integer, Integer>();

        int validPropertyCount = 0;
        final Collection<Property> props = computedDatas[0].featureProperties;
        final Property[] properties = props.toArray(new Property[props.size()]);
        for (int i = 0; i < properties.length; i++) {
            final String name = properties[i].getName().toString();
            if (!correlativDataName.equals(name)) {
                colNames.add(name);
                propertyIndices.put(colStart + validPropertyCount, i);
                validPropertyCount++;
            }
        }
    }

    @Override
    public void encodeCsv(Writer writer) throws IOException {
        new TableModelCsvEncoder(this).encodeCsv(writer);
    }

    @Override
    public int getRowCount() {
        return computedDatas.length;
    }

    @Override
    public int getColumnCount() {
        return colNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return colNames.get(column);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return rowIndex + 1;
        } else if (columnIndex == 1) {
            return computedDatas[rowIndex].x;
        } else if (columnIndex == 2) {
            return computedDatas[rowIndex].y;
        } else if (columnIndex == 3) {
            return computedDatas[rowIndex].lat;
        } else if (columnIndex == 4) {
            return computedDatas[rowIndex].lon;
        } else if (columnIndex == 5) {
            return computedDatas[rowIndex].rasterMean;
        } else if (columnIndex == 6) {
            return computedDatas[rowIndex].rasterSigma;
        } else if (columnIndex == 7) {
            return computedDatas[rowIndex].correlativeData;
        } else if (columnIndex < getColumnCount()) {
            final Collection<Property> propColl = computedDatas[rowIndex].featureProperties;
            final Property[] properties = propColl.toArray(new Property[propColl.size()]);
            final Integer propertyIndex = propertyIndices.get(columnIndex);
            return properties[propertyIndex].getValue();
        }
        return null;
    }

    public String toCVS() {
        StringWriter sw = new StringWriter();
        try {
            encodeCsv(sw);
            sw.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return sw.toString();
    }
}
