package org.esa.beam.binning.operator.ui;

import com.bc.ceres.swing.ListControlBar;

import javax.swing.JTable;

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
        TableHandler.addRow(table, new Object[]{"variable_" + rows, ""}); /*I18N*/
        return true;

    }

    @Override
    public boolean removeRows(int[] indices) {
        TableHandler.removeRows(table, table.getSelectedRows());
        return true;
    }

    @Override
    public boolean moveRowUp(int index) {
        TableHandler.moveRowsUp(table, table.getSelectedRows());
        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        TableHandler.moveRowsDown(table, table.getSelectedRows());
        return true;
    }

    @Override
    public void updateState(ListControlBar listControlBar) {
    }

}
