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
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;

public class SelectionChangeSupportTest extends TestCase {
    public void testListenerHousekeeping() {
        SelectionChangeSupport ses = new SelectionChangeSupport(null);

        SelectionChangeListener[] listeners = ses.getSelectionChangeListeners();
        assertNotNull(listeners);
        assertEquals(0, listeners.length);

        MySelectionChangeListener listener1 = new MySelectionChangeListener();
        ses.addSelectionChangeListener(listener1);
        listeners = ses.getSelectionChangeListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.length);
        assertSame(listener1, listeners[0]);

        MySelectionChangeListener listener2 = new MySelectionChangeListener();
        ses.addSelectionChangeListener(listener2);
        listeners = ses.getSelectionChangeListeners();
        assertNotNull(listeners);
        assertEquals(2, listeners.length);
        assertSame(listener1, listeners[0]);
        assertSame(listener2, listeners[1]);
    }

    public void testListenerDispatching() {
        SelectionChangeEvent event;

        Object eventSource1 = "A";
        Object eventSource2 = "B";

        testExpliciteEvent(new SelectionChangeSupport(), eventSource1);
        testExpliciteEvent(new SelectionChangeSupport(null), eventSource1);
        testExpliciteEvent(new SelectionChangeSupport("A"), eventSource1);

        testExpliciteEvent(new SelectionChangeSupport(), eventSource2);
        testExpliciteEvent(new SelectionChangeSupport(null), eventSource2);
        testExpliciteEvent(new SelectionChangeSupport("A"), eventSource2);


        SelectionChangeSupport selectionChangeSupport;

        selectionChangeSupport = new SelectionChangeSupport();
        testImpliciteEvent(selectionChangeSupport, selectionChangeSupport);
        selectionChangeSupport = new SelectionChangeSupport(null);
        testImpliciteEvent(selectionChangeSupport, selectionChangeSupport);
        selectionChangeSupport = new SelectionChangeSupport(eventSource1);
        testImpliciteEvent(selectionChangeSupport, eventSource1);
        selectionChangeSupport = new SelectionChangeSupport(eventSource2);
        testImpliciteEvent(selectionChangeSupport, eventSource2);
    }

    private Selection testExpliciteEvent(SelectionChangeSupport ses, Object expectedSource) {
        SelectionContext selectionContext = new DefaultSelectionContext();
        Selection selection = new DefaultSelection("A");

        MySelectionChangeListener listener1 = new MySelectionChangeListener();
        MySelectionChangeListener listener2 = new MySelectionChangeListener();
        ses.addSelectionChangeListener(listener1);
        ses.addSelectionChangeListener(listener2);

        ses.fireSelectionChange(new SelectionChangeEvent(expectedSource, selectionContext, selection));
        assertNotNull(listener1.event);
        assertNotNull(listener2.event);
        assertSame(expectedSource, listener1.event.getSource());
        assertSame(expectedSource, listener2.event.getSource());
        assertSame(selectionContext, listener1.event.getSelectionContext());
        assertSame(selectionContext, listener2.event.getSelectionContext());
        assertSame(selection, listener1.event.getSelection());
        assertSame(selection, listener2.event.getSelection());

        return selection;
    }

    private Selection testImpliciteEvent(SelectionChangeSupport ses, Object expectedSource) {
        SelectionContext selectionContext = new DefaultSelectionContext();
        Selection selection = new DefaultSelection("A");

        MySelectionChangeListener listener1 = new MySelectionChangeListener();
        MySelectionChangeListener listener2 = new MySelectionChangeListener();
        ses.addSelectionChangeListener(listener1);
        ses.addSelectionChangeListener(listener2);

        ses.fireSelectionChange(selectionContext, selection);
        assertNotNull(listener1.event);
        assertNotNull(listener2.event);
        assertSame(expectedSource, listener1.event.getSource());
        assertSame(expectedSource, listener2.event.getSource());
        assertSame(selectionContext, listener1.event.getSelectionContext());
        assertSame(selectionContext, listener2.event.getSelectionContext());
        assertSame(selection, listener1.event.getSelection());
        assertSame(selection, listener2.event.getSelection());

        return selection;
    }

    private static class MySelectionChangeListener implements SelectionChangeListener {
        SelectionChangeEvent event;

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            this.event = event;
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
            this.event = event;
        }
    }
}