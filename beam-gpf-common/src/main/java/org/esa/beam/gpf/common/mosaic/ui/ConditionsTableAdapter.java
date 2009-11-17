package org.esa.beam.gpf.common.mosaic.ui;

import org.esa.beam.gpf.common.mosaic.MosaicOp;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

class ConditionsTableAdapter extends AbstractTableAdapter {

    ConditionsTableAdapter(JTable table) {
        super(table);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        final TableModel tableModel = (TableModel) e.getSource();
        final MosaicOp.Condition[] conditions = new MosaicOp.Condition[tableModel.getRowCount()];
        for (int i = 0; i < conditions.length; i++) {
            conditions[i] = new MosaicOp.Condition(tableModel.getValueAt(i, 0).toString(),
                                                   tableModel.getValueAt(i, 1).toString(),
                                                   (Boolean) tableModel.getValueAt(i, 2));
        }
        getBinding().setPropertyValue(conditions);
    }

    @Override
    protected TableModel createTableModel(Object data) {
        if (data instanceof MosaicOp.Condition[]) {
            final MosaicOp.Condition[] conditions = (MosaicOp.Condition[]) data;
            final TableModel tableModel = createTableModel(conditions.length);
            for (int i = 0; i < conditions.length; i++) {
                final MosaicOp.Condition variable = conditions[i];
                tableModel.setValueAt(variable.getName(), i, 0);
                tableModel.setValueAt(variable.getExpression(), i, 1);
                tableModel.setValueAt(variable.isOutput(), i, 2);
            }
            return tableModel;
        }
        return createTableModel(0);
    }

    private TableModel createTableModel(int rowCount) {
        // for adding and removing rows it is important to create a DefaultTableModel
        return new DefaultTableModel(new String[]{"Name", "Expression", "Output"}, rowCount);
    }
}
