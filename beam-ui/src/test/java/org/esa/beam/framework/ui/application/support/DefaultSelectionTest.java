package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;
import org.esa.beam.framework.ui.application.Selection;

public class DefaultSelectionTest  extends TestCase {
    public void testEMPTY() {
        assertTrue(DefaultSelection.EMPTY.isEmpty());
        assertNotNull(DefaultSelection.EMPTY.getElements());
        assertEquals(0, DefaultSelection.EMPTY.getElements().length);
        assertEquals(null, DefaultSelection.EMPTY.getFirstElement());
    }

    public void testEmptySelection() {
        DefaultSelection selection = new DefaultSelection(null);
        assertTrue(selection.isEmpty());
        assertEquals(0, selection.getElementCount());
        assertNull(selection.getFirstElement());

        selection = new DefaultSelection(new Object[0]);
        assertTrue(selection.isEmpty());
        assertEquals(0, selection.getElementCount());
        assertNull(selection.getFirstElement());
    }

    public void testNonEmptySelection() {
        DefaultSelection selection = new DefaultSelection("X");
        assertFalse(selection.isEmpty());
        assertEquals(1, selection.getElementCount());
        assertEquals("X", selection.getFirstElement());
        assertEquals("X", selection.getElement(0));

        selection = new DefaultSelection(new Object[] {"A", "B"});
        assertFalse(selection.isEmpty());
        assertEquals(2, selection.getElementCount());
        assertEquals("A", selection.getFirstElement());
        assertEquals("A", selection.getElement(0));
        assertEquals("B", selection.getElement(1));
    }
}
