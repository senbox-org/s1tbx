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

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * A selection provider that wraps a {@link javax.swing.JList}.
 * Elements contained in {@link Selection}s handled by this provider
 * represent currently selected list objects.
 */
public class ListSelectionContext extends AbstractSelectionContext {

    private final ListSelectionListener listSelectionListener;
    private JList list;

    public ListSelectionContext(final JList list) {
        listSelectionListener = new ListSelectionHandler();
        this.list = list;
        this.list.addListSelectionListener(listSelectionListener);
    }

    @Override
    public Selection getSelection() {
        if (list.getSelectedIndex() == -1) {
            return DefaultSelection.EMPTY;
        }
        return new DefaultSelection<Object>(list.getSelectedValues());
    }

    @Override
    public void setSelection(Selection selection) {
        if (selection.isEmpty()) {
            list.clearSelection();
            return;
        }

        final ListModel listModel = list.getModel();
        final Object[] selectedElements = selection.getSelectedValues();
        int[] indices = new int[selectedElements.length];
        int indexCount = 0;
        for (int i = 0; i < listModel.getSize(); i++) {
            final Object element = listModel.getElementAt(i);
            for (Object selectedElement : selectedElements) {
                if (element.equals(selectedElement)) {
                    indices[indexCount++] = i;
                    break;
                }
            }
        }
        if (indexCount == 0) {
            list.clearSelection();
            return;
        }

        if (indexCount < indices.length) {
            int[] t = new int[indexCount];
            System.arraycopy(indices, 0, t, 0, indexCount);
            indices = t;
        }
        list.setValueIsAdjusting(true);
        list.setSelectedIndices(indices);
        list.setValueIsAdjusting(false);
    }
    @Override
    public void insert(Transferable transferable) throws IOException, UnsupportedFlavorException {
        Object transferData = transferable.getTransferData(DataFlavor.stringFlavor);
        if (transferData != null) {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex >= 0) {
                ((DefaultListModel) list.getModel()).add(selectedIndex, transferData.toString());
            } else {
                ((DefaultListModel) list.getModel()).addElement(transferData.toString());
            }
        }
    }

    @Override
    public boolean canInsert(Transferable contents) {
        return isListEditable()
                && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
    }

    @Override
    public boolean canDeleteSelection() {
        return !getSelection().isEmpty() && isListEditable();
    }

    @Override
    public void deleteSelection() {
        Object[] items = getList().getSelectedValues();
        for (Object item : items) {
            ((DefaultListModel) getList().getModel()).removeElement(item);
        }
        // todo - post undoable edit
    }

    boolean isListEditable() {
        return getList().getModel() instanceof DefaultListModel;
    }
    
    public JList getList() {
        return list;
    }

    public void setList(JList list) {
        if (list != this.list) {
            this.list.removeListSelectionListener(listSelectionListener);
            this.list = list;
            this.list.addListSelectionListener(listSelectionListener);
            fireSelectionChange(getSelection());
        }
    }

    protected void handleListSelectionChange(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            fireSelectionChange(getSelection());
        }
    }

    private class ListSelectionHandler implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent event) {
            handleListSelectionChange(event);
        }
    }
}
