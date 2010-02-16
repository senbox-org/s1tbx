package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionManager;

import javax.swing.KeyStroke;
import java.awt.datatransfer.Transferable;

/**
 * A generic 'copy' action.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class CopyAction extends AbstractSelectionAction {
    public CopyAction(SelectionManager selectionManager) {
        super(selectionManager, "Copy", KeyStroke.getKeyStroke("control C"), "edit-copy.png");
        updateState();
    }

    @Override
    public boolean isExecutable() {
        return super.isExecutable() && !getSelection().isEmpty();
    }

    @Override
    public void execute() {
        Selection selection = getSelection();
        Transferable transferable = selection.createTransferable(true);
        if (transferable != null) {
            getClipboard().setContents(transferable, selection);
        }
    }
}
