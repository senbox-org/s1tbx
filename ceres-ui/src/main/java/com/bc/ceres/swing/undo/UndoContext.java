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