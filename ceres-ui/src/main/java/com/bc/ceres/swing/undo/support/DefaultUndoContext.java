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

package com.bc.ceres.swing.undo.support;

import com.bc.ceres.swing.undo.UndoContext;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;


public class DefaultUndoContext implements UndoContext {
    private final UndoManager undoManager;
    private final UndoableEditSupport undoableEditSupport;

    public DefaultUndoContext(Object source) {
        this(source, new UndoManager());
    }

    public DefaultUndoContext(Object source, UndoManager undoManager) {
        this.undoManager = undoManager;
        this.undoableEditSupport = new UndoableEditSupport(source != null ? source : this);
        this.undoableEditSupport.addUndoableEditListener(undoManager);
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    @Override
    public boolean canUndo() {
        return undoManager.canUndo();
    }

    @Override
    public void undo() {
        undoManager.undo();
    }

    @Override
    public boolean canRedo() {
        return undoManager.canRedo();
    }

    @Override
    public void redo() {
        undoManager.redo();
    }

    @Override
    public void postEdit(UndoableEdit edit) {
        undoableEditSupport.postEdit(edit);
    }

    @Override
    public void addUndoableEditListener(UndoableEditListener listener) {
        undoableEditSupport.addUndoableEditListener(listener);
    }

    @Override
    public void removeUndoableEditListener(UndoableEditListener listener) {
        undoableEditSupport.removeUndoableEditListener(listener);
    }

    @Override
    public UndoableEditListener[] getUndoableEditListeners() {
        return undoableEditSupport.getUndoableEditListeners();
    }
}
