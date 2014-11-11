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
 * An event indicating that a selection or selection context change has occurred.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class SelectionChangeEvent extends java.util.EventObject {
    private final SelectionContext selectionContext;
    private final Selection selection;

    /**
     * Constructs a selection event.
     *
     * @param source           The object that originated the event.
     * @param selectionContext The selection context in which the selection event took place.
     * @param selection        The selection.
     */
    public SelectionChangeEvent(Object source, SelectionContext selectionContext, Selection selection) {
        super(source);
        this.selectionContext = selectionContext;
        this.selection = selection;
    }

    /**
     * @return The selection context in which the selection event took place.
     */
    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    /**
     * @return The selection.
     */
    public Selection getSelection() {
        return selection;
    }
}

