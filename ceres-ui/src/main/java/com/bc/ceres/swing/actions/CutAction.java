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
