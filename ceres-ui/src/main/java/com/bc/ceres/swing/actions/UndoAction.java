package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.undo.UndoContext;

import javax.swing.KeyStroke;

public class UndoAction extends AbstractUndoAction {

    public UndoAction(UndoContext undoContext) {
        super(undoContext, "Undo", KeyStroke.getKeyStroke("control Z"), "edit-undo.png");
        updateState();
    }

    @Override
    public boolean isExecutable() {
        return getUndoContext().canUndo();
    }

    @Override
    public void execute() {
        getUndoContext().undo();
    }
}
