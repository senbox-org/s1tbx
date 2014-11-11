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

import junit.framework.TestCase;
import com.bc.ceres.swing.selection.support.DefaultSelection;
import com.bc.ceres.swing.selection.support.DefaultSelectionContext;

public class SelectionEventTest extends TestCase {
    public void testConstructor() {
        DefaultSelectionContext selectionContext = new DefaultSelectionContext();
        DefaultSelection selection = new DefaultSelection("A");
        SelectionChangeEvent event = new SelectionChangeEvent(this, selectionContext, selection);
        assertSame(this, event.getSource());
        assertSame(selectionContext, event.getSelectionContext());
        assertSame(selection, event.getSelection());
    }
}