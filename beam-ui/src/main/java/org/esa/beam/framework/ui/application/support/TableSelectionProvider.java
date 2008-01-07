package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.support.DefaultSelection;
import org.esa.beam.framework.ui.application.Selection;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TableSelectionProvider extends AbstractSelectionProvider {

    private final ListSelectionListener tableSelectionListener;
    private final TableSelectionModelChangeListener selectionModelChangeListener;
    private JTable table;

    public TableSelectionProvider(final JTable table) {
        selectionModelChangeListener = new TableSelectionModelChangeListener();
        tableSelectionListener = new TableSelectionListener();
        this.table = table;
        installTableListeners();
    }

    public Selection getSelection() {
        int[] indexes = table.getSelectedRows();
        Integer[] elements = new Integer[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            elements[i] = indexes[i];
        }
        return new DefaultSelection(elements);
    }

    public void setSelection(Selection selection) {
        // todo
    }

    public JTable getTable() {
        return table;
    }

    public void setTable(JTable table) {
        if (table != this.table) {
            uninstallTableListeners();
            this.table = table;
            installTableListeners();
            fireSelectionChange(this.table);
        }
    }

    private void installTableListeners() {
        table.getSelectionModel().addListSelectionListener(tableSelectionListener);
        table.addPropertyChangeListener("selectionModel", selectionModelChangeListener);
    }

    private void uninstallTableListeners() {
        table.getSelectionModel().removeListSelectionListener(tableSelectionListener);
        table.removePropertyChangeListener("selectionModel", selectionModelChangeListener);
    }

    private class TableSelectionModelChangeListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            final ListSelectionModel oldSelectionModel = (ListSelectionModel) evt.getOldValue();
            if (oldSelectionModel != null) {
                oldSelectionModel.removeListSelectionListener(tableSelectionListener);
            }
            final ListSelectionModel newSelectionModel = (ListSelectionModel) evt.getNewValue();
            if (newSelectionModel != null) {
                newSelectionModel.addListSelectionListener(tableSelectionListener);
            }
        }
    }

    private class TableSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                fireSelectionChange(table);
            }
        }
    }
}