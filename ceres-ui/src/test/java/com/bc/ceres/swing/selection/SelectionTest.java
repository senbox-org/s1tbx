package com.bc.ceres.swing.selection;

import junit.framework.TestCase;
import com.bc.ceres.swing.selection.support.DefaultSelection;

public class SelectionTest extends TestCase {
    public void testEmpty() {

        assertNotNull(Selection.EMPTY);
        assertEquals(null, Selection.EMPTY.getSelectedValue());
        assertNotNull(Selection.EMPTY.getSelectedValues());
        assertEquals(0, Selection.EMPTY.getSelectedValues().length);
        assertEquals(true, Selection.EMPTY.isEmpty());

        assertEquals("", Selection.EMPTY.getPresentationName());
        assertEquals("Selection.EMPTY", Selection.EMPTY.toString());

        assertNull(Selection.EMPTY.createTransferable(false));
        assertNull(Selection.EMPTY.createTransferable(true));

        assertEquals(false, Selection.EMPTY.equals(null));
        assertEquals(false, Selection.EMPTY.equals("A"));
        assertEquals(false, Selection.EMPTY.equals(new DefaultSelection("A")));
        assertEquals(true, Selection.EMPTY.equals(Selection.EMPTY));
    }
}
