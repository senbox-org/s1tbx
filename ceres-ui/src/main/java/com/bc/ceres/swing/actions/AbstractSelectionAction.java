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
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;

import javax.swing.KeyStroke;
import java.awt.datatransfer.Clipboard;

/**
 * An abstract base class for generic actions which depend and operate on the
 * current selection abstraction.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public abstract class AbstractSelectionAction extends AbstractSystemAction implements SelectionChangeListener {
    private final SelectionManager selectionManager;

    protected AbstractSelectionAction(SelectionManager selectionManager, String name, KeyStroke acceleratorKey, String iconResource) {
        super(name, acceleratorKey, iconResource);
        this.selectionManager = selectionManager;
        this.selectionManager.addSelectionChangeListener(this);
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        // System.out.println(getClass().getSimpleName() + ".selectionChanged: event=" + event);
        updateState();
    }

    @Override
    public void selectionContextChanged(SelectionChangeEvent event) {
        // System.out.println(getClass().getSimpleName() + ".selectionContextChanged: event=" + event);
        updateState();
    }

    /**
     * Checks if a selection context exists.
     * Overrides should first test the return value of {@code super.isExecutable()}.
     * @return {@code true}, if so.
     */
    @Override
    public boolean isExecutable() {
        return getSelectionContext() != null;
    }

    public Selection getSelection() {
        return selectionManager.getSelection();
    }

    public SelectionContext getSelectionContext() {
        return selectionManager.getSelectionContext();
    }

    public Clipboard getClipboard() {
        return selectionManager.getClipboard();
    }

}
