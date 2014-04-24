package org.esa.beam.binning.operator.ui;

import com.bc.ceres.swing.ListControlBar;

import javax.swing.JTable;

/**
 * Controls adding, removing and moving rows in the variable table.
 *
 * @author Tonio Fincke
 */
class VariableTableController extends ListControlBar.AbstractListController {

    final JTable table;

    VariableTableController(JTable table) {
        this.table = table;
    }

    @Override
    public boolean addRow(int index) {
        final int rows = table.getRowCount();
        VariableTableHandler.addRow(table, new Object[]{"variable_" + rows, ""}); /*I18N*/
        return true;

    }

    @Override
    public boolean removeRows(int[] indices) {
        VariableTableHandler.removeRows(table, indices);
        return true;
    }

    @Override
    public boolean moveRowUp(int index) {
        VariableTableHandler.moveRowsUp(table, new int[] {index});
        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        VariableTableHandler.moveRowsDown(table, new int[] {index});
        return true;
    }

    @Override
    public void updateState(ListControlBar listControlBar) {
    }

}
