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

/**
 * Objects implementing this interface emit selection change events to interested
 * {@link SelectionChangeListener selection change listener}s.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface SelectionChangeEmitter {
    /**
     * Adds a selection change listener to this emitter.
     *
     * @param listener The listener.
     */
    void addSelectionChangeListener(SelectionChangeListener listener);

    /**
     * Removes a selection change listener from this emitter.
     *
     * @param listener The listener.
     */
    void removeSelectionChangeListener(SelectionChangeListener listener);

    /**
     * Gets all registered selection change listeners.
     *
     * @return An array containing all registered listeners.
     */
    SelectionChangeListener[] getSelectionChangeListeners();
}