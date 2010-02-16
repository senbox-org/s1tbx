package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.undo.UndoContext;

import javax.swing.KeyStroke;

public class RedoAction extends AbstractUndoAction {
    public RedoAction(UndoContext undoContext) {
        super(undoContext, "Redo", KeyStroke.getKeyStroke("control shift Z"), "edit-redo.png");
        updateState();
    }

    @Override
    public boolean isExecutable() {
        return getUndoContext().canRedo();
    }

    @Override
    public void execute() {
        getUndoContext().redo();
    }
}
