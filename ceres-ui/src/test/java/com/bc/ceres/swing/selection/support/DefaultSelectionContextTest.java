package com.bc.ceres.selection.support;

import junit.framework.TestCase;

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
