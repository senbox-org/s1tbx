package org.esa.beam.visat.toolviews.stat;

import java.io.IOException;
import java.io.Writer;
import javax.swing.table.TableModel;
import org.esa.beam.util.io.CsvWriter;

class TableModelCsvEncoder implements CsvEncoder {

    private final TableModel model;

    public TableModelCsvEncoder(TableModel model) {
        this.model = model;
    }

    @Override
    public void encodeCsv(Writer writer) throws IOException {
        CsvWriter csv = new CsvWriter(writer, "\t");
        encodeHeadline(csv);
        encodeData(csv);
    }

    private void encodeHeadline(CsvWriter csv) throws IOException {
        int count = model.getColumnCount();
        final String[] colNames = new String[count];
        for (int col = 0; col < count; col++) {
            colNames[col] = model.getColumnName(col);
        }
        csv.writeRecord(colNames);
    }

    private void encodeData(CsvWriter csv) throws IOException {
        int columnCount = model.getColumnCount();
        int rowCount = model.getRowCount();
        for (int row = 0; row < rowCount; row++) {
            String[] record = new String[columnCount];
            for (int column = 0; column < columnCount; column++) {
                Object value = model.getValueAt(row, column);
                record[column] = value != null ? value.toString() : "";
            }
            csv.writeRecord(record);
        }
    }
}
