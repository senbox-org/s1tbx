package org.esa.beam.gpf.operators.mosaic;

import org.esa.beam.gpf.operators.standard.MosaicOp;

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
            variables[i] = new MosaicOp.Variable((String) tableModel.getValueAt(i, 0),
                                                 (String) tableModel.getValueAt(i, 1));
        }
        getBinding().setPropertyValue(variables);
    }

    @Override
    protected final DefaultTableModel createTableModel(int rowCount) {
        return new DefaultTableModel(new String[]{"Name", "Expression"}, rowCount);
    }
}
