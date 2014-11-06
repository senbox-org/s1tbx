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

import com.bc.ceres.swing.undo.UndoContext;

import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.UndoManager;

public abstract class AbstractUndoAction extends AbstractSystemAction implements UndoableEditListener {
    private final UndoContext undoContext;

    public AbstractUndoAction(UndoContext undoContext, String name, KeyStroke acceleratorKey, String iconResource) {
        super(name, acceleratorKey, iconResource);
        this.undoContext = undoContext;
        this.undoContext.addUndoableEditListener(this);
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        updateState();
    }

    public UndoContext getUndoContext() {
        return undoContext;
    }
}
