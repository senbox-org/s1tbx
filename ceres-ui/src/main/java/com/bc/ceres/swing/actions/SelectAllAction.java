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
