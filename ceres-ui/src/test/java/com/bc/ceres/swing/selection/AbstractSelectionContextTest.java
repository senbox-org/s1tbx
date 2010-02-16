package com.bc.ceres.swing.selection;

import com.bc.ceres.swing.selection.support.DefaultSelection;
import junit.framework.TestCase;

import java.awt.datatransfer.StringSelection;

public class AbstractSelectionContextTest extends TestCase {
    public void testDefaultOperations() {
        MySelectionContext context = new MySelectionContext();

        assertEquals(false, context.canInsert(null));
        try {
            context.insert(new StringSelection("X"));
            fail("Exception expected");
        } catch (Exception e) {
            // ok
        }

        assertEquals(false, context.canDeleteSelection());
        try {
            context.deleteSelection();
            fail("Exception expected");
        } catch (Exception e) {
            // ok
        }

        assertEquals(false, context.canSelectAll());
        try {
            context.selectAll();
            fail("Exception expected");
        } catch (Exception e) {
            // ok
        }
    }

    public void testSelectionChangeSupport() {
        MySelectionContext context = new MySelectionContext();

        assertNotNull(context.getSelectionChangeListeners());
        assertEquals(0, context.getSelectionChangeListeners().length);

        TracingSelectionChangeListener listener1 = new TracingSelectionChangeListener();
        context.addSelectionChangeListener(listener1);

        assertNotNull(context.getSelectionChangeListeners());
        assertEquals(1, context.getSelectionChangeListeners().length);
        assertSame(listener1, context.getSelectionChangeListeners()[0]);

        TracingSelectionChangeListener listener2 = new TracingSelectionChangeListener();
        context.addSelectionChangeListener(listener2);
        assertNotNull(context.getSelectionChangeListeners());
        assertEquals(2, context.getSelectionChangeListeners().length);
        assertSame(listener1, context.getSelectionChangeListeners()[0]);
        assertSame(listener2, context.getSelectionChangeListeners()[1]);

        context.removeSelectionChangeListener(listener2);
        context.removeSelectionChangeListener(listener1);
        assertNotNull(context.getSelectionChangeListeners());
        assertEquals(0, context.getSelectionChangeListeners().length);

        context.addSelectionChangeListener(listener1);
        context.fireSelectionChange(new DefaultSelection("A"));
        assertEquals("sc(A);", listener1.trace);
        context.fireSelectionChange(new DefaultSelection("B"));
        assertEquals("sc(A);sc(B);", listener1.trace);
        context.fireSelectionChange(Selection.EMPTY);
        assertEquals("sc(A);sc(B);sc();", listener1.trace);
    }

    private static class MySelectionContext extends AbstractSelectionContext {
        @Override
        public void setSelection(Selection selection) {
        }

        @Override
        public Selection getSelection() {
            return null;
        }

        @Override
        public void fireSelectionChange(Selection selection) {
            super.fireSelectionChange(selection);
        }
    }

}