package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.undo.UndoContext;

import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;

public abstract class AbstractUndoAction extends AbstractSystemAction implements UndoableEditListener {
    private final UndoContext undoContext;

    public AbstractUndoAction(UndoContext undoContext, String name, KeyStroke acceleratorKey, String iconResource) {
        super(name, acceleratorKey, iconResource);
        this.undoContext = undoContext;
        this.undoContext.addUndoableEditListener(this);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        updateState();
    }

    public UndoContext getUndoContext() {
        return undoContext;
    }
}
