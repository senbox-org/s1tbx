package org.esa.beam.visat.toolviews.stat;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.swing.table.AbstractTableModel;
import org.jfree.data.xy.XYIntervalSeries;

/**
 * @author Sabine Embacher
 */
public class ScatterPlotTableModel extends AbstractTableModel implements CsvEncoder {

    private final String[] colNames;
    private final int boxSize;
    private final Location[] locations;

    public ScatterPlotTableModel(String rasterName, String trackDataName,
                                 Location[] locations, int boxSize) {

        colNames = new String[]{
                "pixel_no",
                "pixel_x",
                "pixel_y",
                "latitude",
                "longitude",
                "box_size",
                rasterName + "_mean",
                rasterName + "_sigma",
                trackDataName + "_mean",
                trackDataName + "_sigma"};

        this.boxSize = boxSize;
        this.locations = locations;
    }

    @Override
    public void encodeCsv(Writer writer) throws IOException {
        new TableModelCsvEncoder(this).encodeCsv(writer);
    }

    @Override
    public int getRowCount() {
        return locations.length;
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
            return locations[rowIndex].x;
        } else if (columnIndex == 2) {
            return locations[rowIndex].y;
        } else if (columnIndex == 3) {
            return locations[rowIndex].lat;
        } else if (columnIndex == 4) {
            return locations[rowIndex].lon;
        } else if (columnIndex == 5) {
            return boxSize;
        } else if (columnIndex == 6) {
            return locations[rowIndex].rasterValue;
        } else if (columnIndex == 7) {
            return locations[rowIndex].rasterSigma;
        } else if (columnIndex == 8) {
            return locations[rowIndex].trackDataValue;
        } else if (columnIndex == 9) {
            return locations[rowIndex].trackDataSigma;
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

    static class Location {
        final float rasterValue;
        final float rasterSigma;
        final float trackDataValue;
        final float trackDataSigma;
        final float x;
        final float y;
        final float lat;
        final float lon;

        Location(float x, float y, float lat, float lon, float rasterValue, float rasterSigma, float trackDataValue, float trackDataSigma) {
            this.x = x;
            this.y = y;
            this.lat = lat;
            this.lon = lon;
            this.rasterValue = rasterValue;
            this.rasterSigma = rasterSigma;
            this.trackDataValue = trackDataValue;
            this.trackDataSigma = trackDataSigma;
        }
    }
}
