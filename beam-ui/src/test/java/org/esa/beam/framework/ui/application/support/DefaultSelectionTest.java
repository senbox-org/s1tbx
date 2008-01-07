package org.esa.beam.framework.ui.application.support;

import junit.framework.TestCase;

public class DefaultSelectionTest extends TestCase {
    public void testEMPTY() {
        assertTrue(DefaultSelection.EMPTY.isEmpty());
        assertNotNull(DefaultSelection.EMPTY.getElements());
        assertEquals(0, DefaultSelection.EMPTY.getElements().length);
        assertEquals(null, DefaultSelection.EMPTY.getFirstElement());
    }

    public void testEmptySelection() {
        DefaultSelection selection = new DefaultSelection(null);
        assertTrue(selection.isEmpty());
        assertNull(selection.getFirstElement());

        selection = new DefaultSelection(new Object[0]);
        assertTrue(selection.isEmpty());
        assertNull(selection.getFirstElement());
    }

    public void testNonEmptySelection() {
        DefaultSelection selection = new DefaultSelection("X");
        assertFalse(selection.isEmpty());
        assertEquals("X", selection.getFirstElement());

        selection = new DefaultSelection(new Object[]{"A", "B"});
        assertFalse(selection.isEmpty());
        assertEquals("A", selection.getFirstElement());
    }

    public void testEquals() {
        assertTrue(new DefaultSelection(null).equals(DefaultSelection.EMPTY));
        assertTrue(new DefaultSelection(new Object[]{}).equals(DefaultSelection.EMPTY));

        assertTrue(new DefaultSelection(null).equals(new DefaultSelection(null)));
        assertTrue(new DefaultSelection(new Object[]{}).equals(new DefaultSelection(null)));
        assertTrue(new DefaultSelection(null).equals(new DefaultSelection(new Object[]{})));
        assertTrue(new DefaultSelection(new Object[]{}).equals(new DefaultSelection(new Object[]{})));

        assertTrue(new DefaultSelection(new Object[]{"A", "B", "C"}).equals(
                new DefaultSelection(new Object[]{"A", "B", "C"})));


        assertFalse(new DefaultSelection(new Object[]{}).equals(
                new DefaultSelection(new Object[]{"A"})));

        assertFalse(new DefaultSelection(new Object[]{"B", "A", "C"}).equals(
                new DefaultSelection(new Object[]{"A", "B", "C"})));

    }
}
