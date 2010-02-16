package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionManager;

import javax.swing.KeyStroke;
import java.awt.datatransfer.Transferable;

/**
 * A generic 'cut' action.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class CutAction extends AbstractSelectionAction {
    public CutAction(SelectionManager selectionManager) {
        super(selectionManager, "Cut", KeyStroke.getKeyStroke("control X"), "edit-cut.png");
        updateState();
    }

    @Override
    public boolean isExecutable() {
        return super.isExecutable()
                && !getSelection().isEmpty()
                && getSelectionContext().canDeleteSelection();
    }

    @Override
    public void execute() {
        Selection selection = getSelection();
        Transferable transferable = selection.createTransferable(false);
        if (transferable != null) {
            getClipboard().setContents(transferable, selection);
            getSelectionContext().deleteSelection();
        }
    }
}
