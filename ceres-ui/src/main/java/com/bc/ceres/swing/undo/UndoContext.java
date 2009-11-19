package com.bc.ceres.swing.undo;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

/**
 * @see javax.swing.undo.UndoManager
 * @see javax.swing.undo.UndoableEditSupport
 */
public interface UndoContext {
    UndoManager getUndoManager();

    void postEdit(UndoableEdit edit);

    void addUndoableEditListener(UndoableEditListener listener);

    void removeUndoableEditListener(UndoableEditListener listener);
}