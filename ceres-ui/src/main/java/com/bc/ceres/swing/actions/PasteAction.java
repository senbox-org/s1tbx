package com.bc.ceres.swing.actions;

import com.bc.ceres.selection.SelectionChangeEvent;
import com.bc.ceres.selection.SelectionManager;

import javax.swing.KeyStroke;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;

/**
 * A generic 'paste' action.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */

public class PasteAction extends AbstractSelectionAction implements FlavorListener {

    public PasteAction(SelectionManager selectionManager) {
        super(selectionManager, "Paste", KeyStroke.getKeyStroke("control V"), "edit-paste.png");
        getClipboard().addFlavorListener(this);
        updateState();
    }

    @Override
    public boolean isExecutable() {
        Transferable contents = getClipboard().getContents(this);
        return getSelectionContext().canInsert(contents);
    }

    @Override
    public void execute() {
        Transferable contents = getClipboard().getContents(this);
        try {
            getSelectionContext().insert(contents);
        } catch (Exception e) {
            handleInsertProblem(e);
        }
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        // Overridden to do nothing, since this action is not
        // dependent on selection changes.
    }

    @Override
    public void flavorsChanged(FlavorEvent e) {
        System.out.println(getClass().getSimpleName() + ".flavorsChanged: e = " + e);
        updateState();
    }

    protected void handleInsertProblem(Exception e) {
    }
}
