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

package com.bc.ceres.swing.selection;

import com.bc.ceres.core.ExtensibleObject;
import com.bc.ceres.swing.selection.support.SelectionChangeSupport;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * A handy base class for implementations of the {@link SelectionContext} interface.
 * <p>Clients only need to implement the methods {@link #getSelection()}
 * and {@link #setSelection(Selection)}.
 * <p>No particular selection operations are implemented, e.g. {@link #canDeleteSelection()},
 * {@link #canInsert(java.awt.datatransfer.Transferable)}, {@link #canSelectAll()},
 * all return {@code false}.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public abstract class AbstractSelectionContext extends ExtensibleObject implements SelectionContext {
    private final SelectionChangeSupport selectionChangeSupport;

    protected AbstractSelectionContext() {
        this(null);
    }

    protected AbstractSelectionContext(Object selectionSource) {
        Object realSource = selectionSource != null ? selectionSource : this;
        selectionChangeSupport = new SelectionChangeSupport(realSource);
    }

    @Override
    public boolean canInsert(Transferable contents) {
        return false;
    }

    @Override
    public void insert(Transferable transferable) throws IOException, UnsupportedFlavorException {
        throw new IllegalStateException("Unsupported operation.");
    }

    @Override
    public boolean canDeleteSelection() {
        return false;
    }

    @Override
    public void deleteSelection() {
        throw new IllegalStateException("Unsupported operation.");
    }

    @Override
    public boolean canSelectAll() {
        return false;
    }

    @Override
    public void selectAll() {
        throw new IllegalStateException("Unsupported operation.");
    }

    @Override
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.addSelectionChangeListener(listener);
    }

    @Override
    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.removeSelectionChangeListener(listener);
    }

    @Override
    public SelectionChangeListener[] getSelectionChangeListeners() {
        return selectionChangeSupport.getSelectionChangeListeners();
    }

    protected void fireSelectionChange(Selection selection) {
        selectionChangeSupport.fireSelectionChange(this, selection);
    }
}