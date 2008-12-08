package org.esa.beam.smos.visat;

import javax.swing.table.AbstractTableModel;


class SnapshotTableModel extends AbstractTableModel {
    Object[][] objects;

    public SnapshotTableModel(Object[][] objects) {
        this.objects = objects;
    }

    public int getRowCount() {
        return objects.length;
    }

    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "Name" : "Value";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        return objects[rowIndex][columnIndex];
    }
}
