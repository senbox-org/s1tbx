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

package com.bc.ceres.swing.selection.support;

import junit.framework.TestCase;

import java.awt.datatransfer.DataFlavor;

public class DefaultSelectionTest extends TestCase {

    public void testEmpty() {
        DefaultSelection selection = new DefaultSelection();

        assertEquals(true, selection.isEmpty());
        assertEquals(null, selection.getSelectedValue());
        assertNotNull(selection.getSelectedValues());
        assertEquals(0, selection.getSelectedValues().length);
        assertEquals("", selection.getPresentationName());

        assertNull(selection.createTransferable(false));
        assertNull(selection.createTransferable(true));

        assertEquals("DefaultSelection[selectedValues={}]", selection.toString());

        assertEquals(false, selection.equals(null));
        assertEquals(true, selection.equals(selection));
        assertEquals(true, selection.equals(new DefaultSelection()));
        assertEquals(false, selection.equals(new DefaultSelection("B")));
    }

    public void testOneElement() {
        DefaultSelection selection = new DefaultSelection("X");

        assertEquals(false, selection.isEmpty());
        assertEquals("X", selection.getSelectedValue());
        assertNotNull(selection.getSelectedValues());
        assertEquals(1, selection.getSelectedValues().length);
        assertEquals("X", selection.getSelectedValues()[0]);
        assertEquals("X", selection.getPresentationName());

        assertNotNull(selection.createTransferable(false));
        assertNotNull(selection.createTransferable(true));
        assertEquals(true, selection.createTransferable(true).isDataFlavorSupported(DataFlavor.stringFlavor));

        assertEquals("DefaultSelection[selectedValues={X}]", selection.toString());

        assertEquals(false, selection.equals(null));
        assertEquals(true, selection.equals(selection));
        assertEquals(true, selection.equals(new DefaultSelection("X")));
        assertEquals(false, selection.equals(new DefaultSelection("B")));
    }

    public void testMoreElements() {
        DefaultSelection selection = new DefaultSelection("A", "B", "C");

        assertEquals(false, selection.isEmpty());
        assertEquals("A", selection.getSelectedValue());
        assertNotNull(selection.getSelectedValues());
        assertEquals(3, selection.getSelectedValues().length);
        assertEquals("A", selection.getSelectedValues()[0]);
        assertEquals("B", selection.getSelectedValues()[1]);
        assertEquals("C", selection.getSelectedValues()[2]);
        assertEquals("A", selection.getPresentationName());

        assertNotNull(selection.createTransferable(false));
        assertNotNull(selection.createTransferable(true));
        assertEquals(true, selection.createTransferable(true).isDataFlavorSupported(DataFlavor.stringFlavor));

        assertEquals("DefaultSelection[selectedValues={A,B,C}]", selection.toString());

        assertEquals(false, selection.equals(null));
        assertEquals(true, selection.equals(selection));
        assertEquals(true, selection.equals(new DefaultSelection("A", "B", "C")));
        assertEquals(false, selection.equals(new DefaultSelection("B", "A", "C")));
        assertEquals(false, selection.equals(new DefaultSelection("B")));
    }
}