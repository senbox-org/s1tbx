package org.esa.beam.gpf.operators.mosaic;

import org.esa.beam.gpf.operators.standard.MosaicOp;

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
            conditions[i] = new MosaicOp.Condition((String) tableModel.getValueAt(i, 0),
                                                   (String) tableModel.getValueAt(i, 1),
                                                   Boolean.TRUE.equals(tableModel.getValueAt(i, 2)));
        }
        getBinding().setPropertyValue(conditions);
    }

    @Override
    protected final DefaultTableModel createTableModel(int rowCount) {
        return new DefaultTableModel(new String[]{"Name", "Expression", "Output"}, rowCount);
    }
}
