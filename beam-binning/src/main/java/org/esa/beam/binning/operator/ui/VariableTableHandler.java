package org.esa.beam.binning.operator.ui;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Implements adding, removing and moving rows in the variable table.
 *
 * @author Tonio Fincke
 */
class VariableTableHandler {

    public static void addRow(final JTable table, final Object[] rowData) {
        table.removeEditor();
        ((DefaultTableModel) table.getModel()).addRow(rowData);
        final int row = table.getRowCount() - 1;
        selectRows(table, row, row);
    }

    public static void removeRows(final JTable table, final int[] rows) {
        table.removeEditor();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }
    }

    public static void moveRowsDown(final JTable table, final int[] rows) {
        final int maxRow = table.getRowCount() - 1;
        for (int row1 : rows) {
            if (row1 == maxRow) {
                return;
            }
        }
        table.removeEditor();
        int[] selectedRows = rows.clone();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row + 1);
            selectedRows[i] = row + 1;
        }
        selectRows(table, selectedRows);
    }

    public static void moveRowsUp(final JTable table, final int[] rows) {
        for (int row1 : rows) {
            if (row1 == 0) {
                return;
            }
        }
        table.removeEditor();
        int[] selectedRows = rows.clone();
        for (int i = 0; i < rows.length; i++) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row - 1);
            selectedRows[i] = row - 1;
        }
        selectRows(table, selectedRows);
    }

    public static void selectRows(final JTable table, final int[] rows) {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        for (int row : rows) {
            selectionModel.addSelectionInterval(row, row);
        }
    }

    public static void selectRows(JTable table, int min, int max) {
        final int numRows = max + 1 - min;
        if (numRows <= 0) {
            return;
        }
        selectRows(table, prepareRows(numRows, min));
    }

    private static int[] prepareRows(final int numRows, int min) {
        final int[] rows = new int[numRows];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = min + i;
        }
        return rows;
    }

}
