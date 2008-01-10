package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.support.DefaultSelection;
import org.esa.beam.framework.ui.application.Selection;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A selection provider that wraps a {@link JTable}.
 * Elements contained in {@link Selection}s handled by this provider
 * represent currently selected table row index as an {@link Integer} value.
 */
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

    /**
     * Gets the current selection.
     * The default implementation returns the selected row indices of the table.
     * If overridden, make sure also {@link #setSelection(org.esa.beam.framework.ui.application.Selection)} and {@link #handleTableSelectionChanged(javax.swing.event.ListSelectionEvent)}
     * are appropriately overridden.
     * @return The current selection.
     */
    public Selection getSelection() {
        int[] indexes = table.getSelectedRows();
        if (indexes.length == 0) {
            return DefaultSelection.EMPTY;
        }
        Integer[] elements = new Integer[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            elements[i] = indexes[i];
        }
        return new DefaultSelection(elements);
    }

    /**
     * Sets the current selection.
     * The default implementation expects the selected row indices of the table in the given selection.
     * If overridden, make sure also {@link #getSelection()} and {@link #handleTableSelectionChanged(javax.swing.event.ListSelectionEvent)}
     * are appropriately overridden.
     * @param selection The current selection.
     */
    public void setSelection(Selection selection) {
        if (selection.isEmpty()) {
            table.getSelectionModel().clearSelection();
            return;
        }
        final Object[] elements = selection.getElements();
        table.getSelectionModel().setValueIsAdjusting(true);
        table.getSelectionModel().clearSelection();
        for (Object element : elements) {
            final int index = (Integer) element;
            table.addRowSelectionInterval(index, index);
        }
        table.getSelectionModel().setValueIsAdjusting(false);
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

    protected void handleTableSelectionChanged(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            fireSelectionChange(table);
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
            handleTableSelectionChanged(e);
        }
    }
}