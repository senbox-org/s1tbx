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

import junit.framework.TestCase;
import com.bc.ceres.swing.selection.TracingSelectionChangeListener;

public class DefaultSelectionContextTest extends TestCase {
    public void testSelection() {
        DefaultSelectionContext context = new DefaultSelectionContext();
        assertNotNull(context.getSelection());
        assertEquals(true, context.getSelection().isEmpty());

        TracingSelectionChangeListener listener = new TracingSelectionChangeListener();
        context.addSelectionChangeListener(listener);

        DefaultSelection selection = new DefaultSelection("A");
        context.setSelection(selection);
        assertSame(selection, context.getSelection());
        assertEquals("sc(A);", listener.trace);
    }
}
