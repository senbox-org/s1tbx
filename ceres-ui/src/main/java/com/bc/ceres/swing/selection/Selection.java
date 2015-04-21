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

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;

/**
 * Represents a selection of zero, one or more items, e.g. the selected entries of a table,
 * nodes of a tree view or figures of a drawing.
 * <p>This interface may be implemented by clients.
 * <p>Selections should always be implemented as imutable objects.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface Selection extends ClipboardOwner, Cloneable, Extensible {
    /**
     * An empty selection.
     */
    Selection EMPTY = new AbstractSelection() {
        @Override
        public Object getSelectedValue() {
            return null;
        }

        @Override
        public String toString() {
            return "Selection.EMPTY";
        }

        @Override
        public AbstractSelection clone() {
            return (AbstractSelection) EMPTY;
        }
    };

    /**
     * Returns a localized, human-readable description of this selection, suitable
     * for use in a change log or menu entry, for example.
     *
     * @return A presentation name for this selection.
     */
    String getPresentationName();

    /**
     * @return {@code true} if this selection is empty.
     */
    boolean isEmpty();

    /**
     * Gets the selected value.
     * The method returns {@code null} if this selection {@link #isEmpty() is empty}.
     *
     * @return The selected value, or {@code null}.
     */
    Object getSelectedValue();

    /**
     * Gets the selected values of a multiple selection.
     * The method returns an empty array if this selection {@link #isEmpty() is empty}.
     *
     * @return The array of selected values.
     */
    Object[] getSelectedValues();

    /**
     * Creates a transferable representation of this selection.
     *
     * @param snapshot If {@code true}, the returned {@link Transferable} should hold a copy-of rather than a
     *                 reference-to the selection.
     *
     * @return A transferable representation of this selection or {@code null} if this is not possible.
     */
    Transferable createTransferable(boolean snapshot);

    /**
     * Creates and returns a copy of this selection.
     *
     * @return A clone of this selection instance.
     */
    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    Selection clone();
}
