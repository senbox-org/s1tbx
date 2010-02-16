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
