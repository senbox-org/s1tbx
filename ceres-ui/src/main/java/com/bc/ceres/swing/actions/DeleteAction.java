package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.selection.SelectionManager;

import javax.swing.KeyStroke;

/**
 * A generic 'delete' action.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class DeleteAction extends AbstractSelectionAction {
    public DeleteAction(SelectionManager selectionManager) {
        super(selectionManager, "Delete", KeyStroke.getKeyStroke("DELETE"), "edit-delete.png");
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
        getSelectionContext().deleteSelection();
    }
}
