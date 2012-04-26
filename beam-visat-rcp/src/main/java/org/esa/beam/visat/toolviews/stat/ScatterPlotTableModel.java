package org.esa.beam.visat.toolviews.stat;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.swing.table.AbstractTableModel;

/**
 * @author Sabine Embacher
 */
public class ScatterPlotTableModel extends AbstractTableModel implements CsvEncoder {

    private final String[] colNames;
    private final ScatterPlotPanel.ComputedData[] computedDatas;

    public ScatterPlotTableModel(String rasterName, String correlativDataName, ScatterPlotPanel.ComputedData[] computedDatas) {

        colNames = new String[]{
                "pixel_no",
                "pixel_x",
                "pixel_y",
                "latitude",
                "longitude",
                rasterName + "_mean",
                rasterName + "_sigma",
                correlativDataName};

        this.computedDatas = computedDatas;
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
        return colNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return colNames[column];
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
