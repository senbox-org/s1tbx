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

package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.support.DefaultSelectionContext;
import org.junit.Ignore;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;

@Ignore
class TestSelectionContext extends DefaultSelectionContext {
    ArrayList<String> items = new ArrayList<String>();

    TestSelectionContext() {
        items.add("A");
        items.add("B");
        items.add("C");
    }

    @Override
    public boolean canInsert(Transferable contents) {
        boolean flavorSupported = contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        return flavorSupported;
    }

    @Override
    public void insert(Transferable transferable) {
        String value = getValue(transferable);
        items.add(value);
    }

    @Override
    public boolean canDeleteSelection() {
        return items.contains(getSelection().getSelectedValue());
    }

    @Override
    public void deleteSelection() {
        items.remove(getSelection().getSelectedValue());
        setSelection(Selection.EMPTY);
    }

    @Override
    public boolean canSelectAll() {
        return false;
    }

    @Override
    public void selectAll() {
    }

    private String getValue(Transferable transferable) {
        try {
            return (String) transferable.getTransferData(DataFlavor.stringFlavor);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
