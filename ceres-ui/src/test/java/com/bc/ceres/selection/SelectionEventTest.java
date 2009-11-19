package com.bc.ceres.selection;

import junit.framework.TestCase;
import com.bc.ceres.selection.support.DefaultSelection;
import com.bc.ceres.selection.support.DefaultSelectionContext;

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