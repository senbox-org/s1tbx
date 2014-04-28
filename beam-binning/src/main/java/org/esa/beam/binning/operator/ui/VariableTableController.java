package org.esa.beam.binning.operator.ui;

import com.bc.ceres.swing.ListControlBar;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Controls adding, removing and moving rows in the variable table.
 *
 * @author Tonio Fincke
 */
class VariableTableController extends ListControlBar.AbstractListController {

    final JTable table;
    private final BinningFormModel binningModel;

    VariableTableController(JTable table, BinningFormModel binningFormModel) {
        this.table = table;
        this.binningModel = binningFormModel;
    }

    @Override
    public boolean addRow(int index) {
        final Product contextProduct = binningModel.getContextProduct();
        if (contextProduct == null) {
            JOptionPane.showMessageDialog(table, "At least one source product must be set first");
            return false;
        }

        final VariableItemDialog variableItemDialog = new VariableItemDialog(SwingUtilities.getWindowAncestor(table), contextProduct);
        if (variableItemDialog.show() == ModalDialog.ID_OK) {
            final VariableConfig variableConfig = variableItemDialog.getVariableConfig();
            if (variableConfig == null) {
                return false;
            }
            String varName = variableConfig.getName();
            VariableTableHandler.addRow(table, new Object[]{varName, variableConfig.getExpr()});
            return true;
        }
        return false;
    }

    @Override
    public boolean removeRows(int[] indices) {
        VariableTableHandler.removeRows(table, indices);
        return true;
    }

    @Override
    public boolean moveRowUp(int index) {
        VariableTableHandler.moveRowsUp(table, new int[]{index});
        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        VariableTableHandler.moveRowsDown(table, new int[]{index});
        return true;
    }

    @Override
    public void updateState(ListControlBar listControlBar) {
    }

    void setVariableConfigs(VariableConfig[] variableConfigs) {
        table.removeEditor();
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.setRowCount(0);
        for (VariableConfig variableConfig : variableConfigs) {
            VariableTableHandler.addRow(table, new Object[]{variableConfig.getName(), variableConfig.getExpr()}); /*I18N*/
        }
    }

}
