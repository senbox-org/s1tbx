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

import java.awt.datatransfer.Clipboard;

/**
 * A selection manager is a {@link SelectionSource selection source} which receives its
 * current selection from a known {@link SelectionContext selection context}.
 * <p>
 * All changes in the current selection context are propagated to the
 * {@link SelectionChangeListener selection change listener}s registered with the selection
 * manager.
 * <p>
 * This interface may be directly implemented by clients, although it is advised
 * to use {@link com.bc.ceres.swing.selection.support.DefaultSelectionManager SelectionManagerImpl},
 * since this interface may evolve in the future.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface SelectionManager extends SelectionSource {

    /**
     * @return The clipboard used by this selection manager.
     *
     * @see java.awt.Toolkit#getSystemClipboard()
     * @see java.awt.Toolkit#getSystemSelection()
     */
    Clipboard getClipboard();

    /**
     * Gets the current selection context.
     *
     * @return The current selection context, or {@code null}.
     */
    SelectionContext getSelectionContext();

    /**
     * Sets a new selection context.
     *
     * @param selectionContext The new selection context, or {@code null}.
     */
    void setSelectionContext(SelectionContext selectionContext);
}
