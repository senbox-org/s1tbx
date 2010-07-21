/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.AbstractSelectionContext;
import com.bc.ceres.swing.selection.Selection;

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
public class TableSelectionContext extends AbstractSelectionContext {

    private final ListSelectionListener tableSelectionListener;
    private final TableSelectionModelChangeListener selectionModelChangeListener;
    private JTable table;

    public TableSelectionContext(final JTable table) {
        selectionModelChangeListener = new TableSelectionModelChangeListener();
        tableSelectionListener = new TableSelectionListener();
        this.table = table;
        installTableListeners();
    }

    /**
     * Gets the current selection.
     * The default implementation returns the selected row indices of the table.
     * If overridden, make sure also {@link #setSelection(Selection)} and {@link #handleTableSelectionChanged(javax.swing.event.ListSelectionEvent)}
     * are appropriately overridden.
     *
     * @return The current selection.
     */
    @Override
    public Selection getSelection() {
        int[] indexes = table.getSelectedRows();
        if (indexes.length == 0) {
            return DefaultSelection.EMPTY;
        }
        Integer[] elements = new Integer[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            elements[i] = indexes[i];
        }
        return new DefaultSelection<Integer>(elements);
    }

    /**
     * Sets the current selection.
     * The default implementation expects the selected row indices of the table in the given selection.
     * If overridden, make sure also {@link #getSelection()} and {@link #handleTableSelectionChanged(javax.swing.event.ListSelectionEvent)}
     * are appropriately overridden.
     *
     * @param selection The current selection.
     */
    @Override
    public void setSelection(Selection selection) {
        if (selection.isEmpty()) {
            table.getSelectionModel().clearSelection();
            return;
        }
        final Object[] elements = selection.getSelectedValues();
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
            fireSelectionChange(getSelection());
        }
    }

    protected void handleTableSelectionChanged(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            fireSelectionChange(getSelection());
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
        @Override
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

        @Override
        public void valueChanged(ListSelectionEvent e) {
            handleTableSelectionChanged(e);
        }
    }
}