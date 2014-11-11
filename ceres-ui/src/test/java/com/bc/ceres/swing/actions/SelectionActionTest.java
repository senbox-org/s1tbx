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

package com.bc.ceres.swing.actions;

import junit.framework.TestCase;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;
import com.bc.ceres.swing.actions.DeleteAction;
import com.bc.ceres.swing.actions.PasteAction;
import com.bc.ceres.swing.selection.support.DefaultSelection;
import com.bc.ceres.swing.selection.support.DefaultSelectionManager;

import javax.swing.Action;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class SelectionActionTest extends TestCase {
    public void testDeleteAction() {
        SelectionManager selectionManager = new DefaultSelectionManager();
        TestSelectionContext context = new TestSelectionContext();
        selectionManager.setSelectionContext(context);
        assertEquals(true, selectionManager.getSelection().isEmpty());

        Action action = new DeleteAction(selectionManager);
        assertEquals(false, action.isEnabled());

        setSelectionAndTestActionState(new DefaultSelection("B"), true, action, context);
        setSelectionAndTestActionState(new DefaultSelection(), false, action, context);
        setSelectionAndTestActionState(new DefaultSelection("B"), true, action, context);
        setSelectionAndTestActionState(new DefaultSelection("Z"), false, action, context);
        setSelectionAndTestActionState(new DefaultSelection("B"), true, action, context);

        action.actionPerformed(null);
        assertEquals(true, context.items.contains("A"));
        assertEquals(false, context.items.contains("B"));
        assertEquals(true, context.items.contains("C"));

        assertEquals(true, context.getSelection().isEmpty());
        assertEquals(false, action.isEnabled());
    }

    public void testPasteAction() {
        SelectionManager selectionManager = new DefaultSelectionManager();
        TestSelectionContext context = new TestSelectionContext();
        selectionManager.setSelectionContext(context);
        assertEquals(true, selectionManager.getSelection().isEmpty());

        DefaultSelection<String> selectionZ = new DefaultSelection<String>("Z");
        Transferable contents = new StringSelection(selectionZ.getSelectedValue());
        selectionManager.getClipboard().setContents(contents, selectionZ);
        assertEquals(true, selectionManager.getClipboard().getContents(null).isDataFlavorSupported(DataFlavor.stringFlavor));

        Action action = new PasteAction(selectionManager);
        assertEquals(true, action.isEnabled());

        // test that PasteAction is independent of currrent selection
        setSelectionAndTestActionState(new DefaultSelection<String>("B"), true, action, context);
        setSelectionAndTestActionState(new DefaultSelection<String>(), true, action, context);
        setSelectionAndTestActionState(new DefaultSelection<String>("Z"), true, action, context);

        action.actionPerformed(null);
        assertEquals(true, context.items.contains("A"));
        assertEquals(true, context.items.contains("B"));
        assertEquals(true, context.items.contains("C"));
        assertEquals(true, context.items.contains("Z"));

        assertEquals(true, action.isEnabled());
    }


    private static void setSelectionAndTestActionState(Selection selection, boolean expectedState, Action action, SelectionContext context) {
        context.setSelection(selection);
        assertEquals(expectedState, action.isEnabled());
    }
}
