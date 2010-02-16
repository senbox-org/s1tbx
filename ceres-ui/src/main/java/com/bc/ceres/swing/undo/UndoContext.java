package com.bc.ceres.swing.undo;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoableEdit;

/**
 * A context providing basic undo/redo functionality.
 *
 * @author Norman Fomferra
 * @see javax.swing.undo.UndoManager
 * @see javax.swing.undo.UndoableEditSupport
 * @since Ceres 0.10
 */
public interface UndoContext {
    boolean canUndo();

    void undo();

    boolean canRedo();

    void redo();

    /**
     * Posts an undoable edit. Listeners will be notified.
     *
     * @param edit The undoable edit.
     */
    void postEdit(UndoableEdit edit);

    /**
     * Adds an undoable edit listener.
     *
     * @param listener The listener.
     */
    void addUndoableEditListener(UndoableEditListener listener);

    /**
     * Removes an undoable edit listener.
     *
     * @param listener The listener.
     */
    void removeUndoableEditListener(UndoableEditListener listener);

    /**
     * Gets the array of all undoable edit listeners.
     *
     * @return The array of listeners or an empty array if no listeners have been added
     */
    UndoableEditListener[] getUndoableEditListeners();
}