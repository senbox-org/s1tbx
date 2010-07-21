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

package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.AbstractSelectionContext;

/**
 * A default implementation of the {@link com.bc.ceres.swing.selection.SelectionContext SelectionContext} interface.
 * This class is actually only useful for testing purposes. Real world implementations
 * of a {@code SelectionContext} will most likely adapt to the selections
 * emitted by dedicated GUI components.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class DefaultSelectionContext extends AbstractSelectionContext {
    private Selection selection;

    public DefaultSelectionContext() {
        this(null);
    }

    public DefaultSelectionContext(Object selectionSource) {
        super(selectionSource);
        selection = Selection.EMPTY;
    }

    @Override
    public Selection getSelection() {
        return selection;
    }

    @Override
    public void setSelection(Selection selection) {
        if (!this.selection.equals(selection)) {
            this.selection = selection;
            fireSelectionChange(selection);
        }
    }

}