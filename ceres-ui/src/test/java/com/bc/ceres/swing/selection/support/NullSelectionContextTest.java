package com.bc.ceres.swing.selection.support;

import junit.framework.TestCase;
import com.bc.ceres.swing.selection.SelectionContext;

import java.awt.datatransfer.StringSelection;

public class NullSelectionContextTest extends TestCase {
    public void testInstance() {
        assertNotNull(NullSelectionContext.INSTANCE);
    }


    public void testDefaultOperations() {
        SelectionContext context = NullSelectionContext.INSTANCE;

        assertEquals(false, context.canInsert(null));
        try {
            context.insert(new StringSelection("X"));
        } catch (Exception e) {
            fail("Exception not expected: " + e);
        }

        assertEquals(false, context.canDeleteSelection());
        try {
            context.deleteSelection();
        } catch (Exception e) {
            fail("Exception not expected: " + e);
        }

        assertEquals(false, context.canSelectAll());
        try {
            context.selectAll();
        } catch (Exception e) {
            fail("Exception not expected: " + e);
        }
    }

}