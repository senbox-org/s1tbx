package com.bc.ceres.swing.selection;

import junit.framework.TestCase;
import com.bc.ceres.swing.selection.AbstractSelection;

public class AbstractSelectionTest extends TestCase {

    public void testDefaultBehaviour() {
        AbstractSelection selection = new MySelection();

        assertEquals("", selection.getPresentationName());
        assertEquals("A", selection.getSelectedValue());
        assertNotNull(selection.getSelectedValues());
        assertEquals(1, selection.getSelectedValues().length);
        assertEquals("A", selection.getSelectedValues()[0]);

        assertNull(selection.createTransferable(false));
        assertNull(selection.createTransferable(true));

        assertNotNull(selection.clone());
        assertTrue(selection.clone() instanceof MySelection);
    }

    private static class MySelection extends AbstractSelection {

        private MySelection() {
        }

        @Override
        public Object getSelectedValue() {
            return "A";
        }

        @Override
        public boolean isEmpty() {
            return false;
        }
    }
}