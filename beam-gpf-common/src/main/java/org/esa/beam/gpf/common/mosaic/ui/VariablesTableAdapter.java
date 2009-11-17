package org.esa.beam.gpf.common.mosaic.ui;

import org.esa.beam.gpf.common.mosaic.MosaicOp;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

class VariablesTableAdapter extends AbstractTableAdapter {

    VariablesTableAdapter(JTable table) {
        super(table);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        final TableModel tableModel = (TableModel) e.getSource();
        final MosaicOp.Variable[] variables = new MosaicOp.Variable[tableModel.getRowCount()];
        for (int i = 0; i < variables.length; i++) {
            variables[i] = new MosaicOp.Variable(tableModel.getValueAt(i, 0).toString(),
                                                 tableModel.getValueAt(i, 1).toString());
        }
        getBinding().setPropertyValue(variables);
    }

    @Override
    protected TableModel createTableModel(Object data) {
        if (data instanceof MosaicOp.Variable[]) {
            final MosaicOp.Variable[] variables = (MosaicOp.Variable[]) data;
            final TableModel tableModel = createTableModel(variables.length);
            for (int i = 0; i < variables.length; i++) {
                final MosaicOp.Variable variable = variables[i];
                tableModel.setValueAt(variable.getName(), i, 0);
                tableModel.setValueAt(variable.getExpression(), i, 1);
            }
            return tableModel;
        }
        return createTableModel(0);
    }

    private TableModel createTableModel(int rowCount) {
        // for adding and removing rows it is important to create a DefaultTableModel
        return new DefaultTableModel(new String[]{"Name", "Expression"}, rowCount);
    }
}
