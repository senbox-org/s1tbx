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

package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.selection.Selection;

/**
 * A selection of figures.
 * <p>
 * Figures added to this collection will automatically be {@link #isSelected() selected}.
 * When removed, they become deselected.
 * <p>
 * A figure selection can have a certain {@link #getSelectionStage() selection stage}.
 * When displayed, a figure selections asks its figures for its {@link Figure#createHandles(int) Handle}s
 * at the given stage.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface FigureSelection extends FigureCollection, Selection {
    /**
     * Gets the current selection stage.
     * The maximum selection stage is given by the selected figure(s).
     *
     * @return The current selection stage.
     * @see Figure#getMaxSelectionStage()
     * @see Figure#createHandles(int)
     */
    int getSelectionStage();

    /**
     * Sets the current selection stage.
     * The maximum selection stage is given by the selected figure(s).
     *
     * @param stage The current selection stage.
     */
    void setSelectionStage(int stage);

    /**
     * Gets the handles associated with the current selection stage.
     * <ol>
     * <li>For a single selection, the handles are the ones created by the selected figure's
     * {@link com.bc.ceres.swing.figure.Figure#createHandles(int)} factory method.</li>
     * <li>For a multiple selection, the handles are the ones created by this figure selection's
     * {@link com.bc.ceres.swing.figure.Figure#createHandles(int)} factory method.</li>
     * <li>If the selection is empty, an empty handle array is returned.</li>
     * </ol>
     *
     * @return The handles associated with the current selection stage.
     *
     * @see #getSelectionStage()
     */
    Handle[] getHandles();

    Handle getSelectedHandle();

    void setSelectedHandle(Handle handle);
}
