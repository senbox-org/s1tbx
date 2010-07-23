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

import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionManager;

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
        try {
            return super.isExecutable() && getSelectionContext().canInsert(getClipboard().getContents(this));
        } catch (Exception ignore) {
            // can happen if other applications block the system clipboard
            return false;
        }
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
        //System.out.println(getClass().getSimpleName() + ".flavorsChanged: e = " + e);
        updateState();
    }

    protected void handleInsertProblem(Exception e) {
    }
}
