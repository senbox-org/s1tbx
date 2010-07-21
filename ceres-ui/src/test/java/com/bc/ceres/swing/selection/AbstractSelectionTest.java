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