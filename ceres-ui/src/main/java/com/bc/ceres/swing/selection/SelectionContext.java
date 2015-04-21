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

import com.bc.ceres.core.Extensible;

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * A selection context is a {@link SelectionSource selection source}
 * with additional capabilities, e.g. insert, delete and select all.
 * It can be seen as the environment in which selections reside and originate,
 * e.g. a GUI table, list, tree, or a drawing of figures.
 * <p>
 * This interface may be directly implemented by clients, although it is advised
 * to extend {@link AbstractSelectionContext AbstractSelectionContext},
 * since this interface may evolve in the future.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface SelectionContext extends SelectionSource, Extensible {

    /**
     * Sets a new selection.
     *
     * @param selection The new selection.
     */
    void setSelection(Selection selection);

    /**
     * Tests whether the current contents (e.g. from clipboard, drag and drop)
     * can be inserted into this selection context.
     *
     * @param contents The contents.
     *
     * @return {@code true} if the given contents can be inserted into this context.
     *
     * @see #insert(Transferable)
     */
    boolean canInsert(Transferable contents);

    /**
     * Inserts a selection represented by the given {@link  Transferable} into this context.
     *
     * @param transferable The transferable object.
     *
     * @throws IOException                if an I/O error occurs
     * @throws UnsupportedFlavorException
     * @see #canInsert(java.awt.datatransfer.Transferable)
     * @see Selection#createTransferable(boolean)
     * @see Selection#lostOwnership(java.awt.datatransfer.Clipboard, java.awt.datatransfer.Transferable)
     */
    void insert(Transferable transferable) throws IOException, UnsupportedFlavorException;

    /**
     * @return {@code true} if the current selection can be deleted.
     *
     * @see #deleteSelection()
     */
    boolean canDeleteSelection();

    /**
     * Deletes the current selection.
     *
     * @see #canDeleteSelection()
     */
    void deleteSelection();

    /**
     * @return {@code true} if all items can be selected.
     *
     * @see #selectAll()
     */
    boolean canSelectAll();

    /**
     * Selects all items.
     *
     * @see #canSelectAll()
     */
    void selectAll();
}
