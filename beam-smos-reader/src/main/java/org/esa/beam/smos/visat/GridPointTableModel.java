package org.esa.beam.smos.visat;

import javax.swing.table.AbstractTableModel;


class GridPointTableModel extends AbstractTableModel {
    private final GridPointDataset ds;

    GridPointTableModel(GridPointDataset ds) {
        this.ds = ds;
    }

    @Override
    public int getRowCount() {
        return ds.data.length;
    }

    @Override
    public int getColumnCount() {
        return ds.columnNames.length + 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return 1 + columnIndex;
        } else {
            return ds.data[rowIndex][columnIndex - 1];
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "Rec#";
        } else {
            return ds.columnNames[columnIndex - 1];
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Integer.class;
        } else {
            return ds.columnClasses[columnIndex - 1];
        }
    }
}
