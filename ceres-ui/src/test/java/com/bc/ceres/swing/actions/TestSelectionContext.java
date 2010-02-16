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
