package org.esa.beam.gpf.common.mosaic.ui;

import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

abstract class AbstractTableAdapter extends ComponentAdapter implements TableModelListener {

    private final JTable table;

    protected AbstractTableAdapter(
            JTable table) {
        this.table = table;
    }

    JTable getTable() {
        return table;
    }

    @Override
    public final JComponent[] getComponents() {
        return new JComponent[]{table};
    }

    @Override
    public final void bindComponents() {
        adjustTableModel();
        table.getModel().addTableModelListener(this);
    }

    @Override
    public final void unbindComponents() {
        table.getModel().removeTableModelListener(this);
    }

    @Override
    public final void adjustComponents() {
        adjustTableModel();
    }

    @Override
    public abstract void tableChanged(TableModelEvent e);

    protected abstract TableModel createTableModel(Object data);

    private void adjustTableModel() {
        getTable().setModel(createTableModel(getBinding().getPropertyValue()));
    }
}
