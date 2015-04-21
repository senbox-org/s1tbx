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
 * A selection source holds a current selection and reports selection change events
 * by emitting them to interested {@link SelectionChangeListener selection change listener}s.
 * <p>
 * This interface may be implemented by clients.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface SelectionSource extends SelectionChangeEmitter {
    /**
     * @return The current selection. The selection may be empty, but never {@code null}.
     *
     * @see Selection#EMPTY
     * @see Selection#isEmpty()
     */
    Selection getSelection();
}
