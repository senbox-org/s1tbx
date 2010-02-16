package com.bc.ceres.swing.undo.support;

import com.bc.ceres.swing.undo.UndoContext;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEditSupport;
import javax.swing.undo.UndoableEdit;
import javax.swing.event.UndoableEditListener;


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
