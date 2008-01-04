package org.esa.beam.framework.ui.application;

import junit.framework.TestCase;

public class SelectionTest extends TestCase {

    public void testNull() {
        final Selection nullSelection = Selection.NULL;

        assertEquals(0, nullSelection.getElementCount());
        assertEquals(null, nullSelection.getFirstElement());
        assertEquals(null, nullSelection.getElement(0));
        assertEquals(null, nullSelection.getElement(1));
        assertTrue(nullSelection.isEmpty());
    }
}
