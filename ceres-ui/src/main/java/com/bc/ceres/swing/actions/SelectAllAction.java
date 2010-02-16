package com.bc.ceres.swing.actions;

import com.bc.ceres.swing.selection.SelectionManager;

import javax.swing.KeyStroke;

/**
 * A generic 'select all' action.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class SelectAllAction extends AbstractSelectionAction {
    public SelectAllAction(SelectionManager selectionManager) {
        super(selectionManager, "Select All", KeyStroke.getKeyStroke("control A"), "edit-select-all.png");
    }

    @Override
    public boolean isExecutable() {
        return super.isExecutable()
                && getSelectionContext().canSelectAll();
    }

    @Override
    public void execute() {
        getSelectionContext().selectAll();
    }
}
