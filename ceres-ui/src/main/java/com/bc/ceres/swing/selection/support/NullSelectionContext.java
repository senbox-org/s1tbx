package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * A {@code null} implementation of the {@link com.bc.ceres.swing.selection.SelectionContext SelectionContext} interface.
 * This singleton class is useful is cases where a {@code SelectionContext} should
 * never be {@code null} (Null-Object Pattern).
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public final class NullSelectionContext implements SelectionContext {
    /**
     * The only instance of this class.
     */
    public static final SelectionContext INSTANCE = new NullSelectionContext();

    private NullSelectionContext() {
    }

    @Override
    public Selection getSelection() {
        return Selection.EMPTY;
    }

    @Override
    public void setSelection(Selection selection) {
    }

    @Override
    public boolean canInsert(Transferable contents) {
        return false;
    }

    @Override
    public void insert(Transferable transferable) throws IOException, UnsupportedFlavorException {
    }

    @Override
    public boolean canDeleteSelection() {
        return false;
    }

    @Override
    public void deleteSelection() {
    }

    @Override
    public boolean canSelectAll() {
        return false;
    }

    @Override
    public void selectAll() {
    }


    @Override
    public void addSelectionChangeListener(SelectionChangeListener listener) {
    }

    @Override
    public void removeSelectionChangeListener(SelectionChangeListener listener) {
    }

    @Override
    public SelectionChangeListener[] getSelectionChangeListeners() {
        return new SelectionChangeListener[0];
    }
}
