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
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.TracingSelectionChangeListener;

public class DefaultSelectionManagerTest extends TestCase {
    public void testDefaults() {
        DefaultSelectionManager sm = new DefaultSelectionManager(this);
        assertNull(sm.getSelectionContext());
        assertNotNull(sm.getSelection());
        assertEquals(true, sm.getSelection().isEmpty());
    }

    public void testSelectionManagement() {
        Selection selectionA = new DefaultSelection("A");
        Selection selectionB = new DefaultSelection("B");

        SelectionContext context1 = new DefaultSelectionContext();
        SelectionContext context2 = new DefaultSelectionContext();

        context1.setSelection(selectionA);
        context2.setSelection(selectionB);

        DefaultSelectionManager manager = new DefaultSelectionManager(this);

        TracingSelectionChangeListener listener = new TracingSelectionChangeListener();
        manager.addSelectionChangeListener(listener);
        assertEquals("", listener.trace);

        manager.setSelectionContext(context1);
        assertSame(context1, manager.getSelectionContext());
        assertEquals(selectionA, manager.getSelection());
        assertEquals("scc;sc(A);", listener.trace);
        listener.trace = "";

        manager.setSelectionContext(context2);
        assertSame(context2, manager.getSelectionContext());
        assertEquals("scc;sc(B);", listener.trace);
        listener.trace = "";

        manager.setSelectionContext(context2);
        assertSame(context2, manager.getSelectionContext());
        assertEquals("", listener.trace);

        context2.setSelection(selectionA);
        assertSame(selectionA, manager.getSelection());
        context2.setSelection(selectionB);
        assertSame(selectionB, manager.getSelection());
        assertEquals("sc(A);sc(B);", listener.trace);
        listener.trace = "";

        context1.setSelection(selectionA);
        assertSame(selectionB, manager.getSelection());
        context1.setSelection(selectionB);
        assertSame(selectionB, manager.getSelection());
        assertEquals("", listener.trace);

        manager.setSelectionContext(context1);
        assertSame(selectionB, manager.getSelection());
        assertEquals("scc;", listener.trace);
        listener.trace = "";

        context1.setSelection(selectionA);
        context1.setSelection(selectionB);
        context1.setSelection(selectionB);
        context1.setSelection(selectionB);
        context1.setSelection(selectionA);
        context1.setSelection(selectionA);
        context1.setSelection(selectionB);
        assertEquals("sc(A);sc(B);sc(A);sc(B);", listener.trace);
    }

    public void testListeners() {

        SelectionChangeListener[] changeListeners;

        DefaultSelectionManager sm = new DefaultSelectionManager(this);
        changeListeners = sm.getSelectionChangeListeners();
        assertNotNull(changeListeners);
        assertEquals(0, changeListeners.length);

        TracingSelectionChangeListener listener1 = new TracingSelectionChangeListener();
        TracingSelectionChangeListener listener2 = new TracingSelectionChangeListener();
        TracingSelectionChangeListener listener3 = new TracingSelectionChangeListener();
        sm.addSelectionChangeListener(listener1);
        sm.addSelectionChangeListener(listener2);
        sm.addSelectionChangeListener(listener3);

        changeListeners = sm.getSelectionChangeListeners();
        assertNotNull(changeListeners);
        assertEquals(3, changeListeners.length);
        assertSame(listener1, changeListeners[0]);
        assertSame(listener2, changeListeners[1]);
        assertSame(listener3, changeListeners[2]);

        sm.removeSelectionChangeListener(listener2);
        changeListeners = sm.getSelectionChangeListeners();
        assertNotNull(changeListeners);
        assertEquals(2, changeListeners.length);
        assertSame(listener1, changeListeners[0]);
        assertSame(listener3, changeListeners[1]);

        sm.removeSelectionChangeListener(listener1);
        sm.removeSelectionChangeListener(listener3);

        changeListeners = sm.getSelectionChangeListeners();
        assertNotNull(changeListeners);
        assertEquals(0, changeListeners.length);
    }
}
