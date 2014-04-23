package org.esa.beam.binning.operator.ui;

import com.bc.ceres.swing.ListControlBar;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/**
 * Created by tonio on 23.04.2014.
 */
class VariableTableController extends ListControlBar.AbstractListController {

    final JTable table;

    VariableTableController(JTable table) {
        this.table = table;
    }

    @Override
    public boolean addRow(int index) {
        final int rows = table.getRowCount();
        addRow(table, new Object[]{"variable_" + rows, ""}); /*I18N*/
        return true;
    }

    private static void addRow(final JTable table, final Object[] rowData) {
        table.removeEditor();
        ((DefaultTableModel) table.getModel()).addRow(rowData);
        final int row = table.getRowCount() - 1;
        final int numCols = table.getColumnModel().getColumnCount();
        for (int i = 0; i < Math.min(numCols, rowData.length); i++) {
            Object o = rowData[i];
            table.setValueAt(o, row, i);
        }
        selectRows(table, row, row);
    }

    @Override
    public boolean removeRows(int[] indices) {
        removeRows(table, table.getSelectedRows());
        return true;
    }

    private static void removeRows(final JTable table, final int[] rows) {
        table.removeEditor();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }
    }

    @Override
    public boolean moveRowUp(int index) {
        moveRowsUp(table, table.getSelectedRows());
        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        moveRowsDown(table, table.getSelectedRows());
        return true;
    }

    @Override
    public void updateState(ListControlBar listControlBar) {
    }

    private static void moveRowsDown(final JTable table, final int[] rows) {
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

    private static void moveRowsUp(final JTable table, final int[] rows) {
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

    private static void selectRows(final JTable table, final int[] rows) {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        for (int row : rows) {
            selectionModel.addSelectionInterval(row, row);
        }
    }

    private static void selectRows(JTable table, int min, int max) {
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
