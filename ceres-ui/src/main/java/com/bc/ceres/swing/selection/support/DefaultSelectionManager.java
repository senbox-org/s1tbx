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

package com.bc.ceres.swing.selection.support;

import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionContext;
import com.bc.ceres.swing.selection.SelectionManager;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;

/**
 * A default implementation of the {@link SelectionManager} interface.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class DefaultSelectionManager implements SelectionManager {
    private final SelectionChangeSupport selectionChangeSupport;
    private final SelectionChangeMulticaster selectionChangeMulticaster;
    private SelectionContext selectionContext;
    private Selection selection;
    private Clipboard clipboard;

    public DefaultSelectionManager() {
        this(null);
    }

    public DefaultSelectionManager(Object realEventSource) {
        Object eventSource = realEventSource != null ? realEventSource : this;
        this.selectionChangeSupport = new SelectionChangeSupport(eventSource);
        this.selectionChangeMulticaster = new SelectionChangeMulticaster();
        this.selectionContext = null;
        this.selection = Selection.EMPTY;
        if (GraphicsEnvironment.isHeadless()) {
            this.clipboard = new Clipboard("HeadlessClipboard");
        } else {
            this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
    }

    @Override
    public Clipboard getClipboard() {
        return clipboard;
    }

    public void setClipboard(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    @Override
    public SelectionContext getSelectionContext() {
        return selectionContext;
    }

    @Override
    public void setSelectionContext(SelectionContext newSelectionContext) {
        SelectionContext oldSelectionContext = this.selectionContext;
        Selection oldSelection = this.selection;
        if (oldSelectionContext != newSelectionContext) {
            if (oldSelectionContext != null) {
                oldSelectionContext.removeSelectionChangeListener(selectionChangeMulticaster);
            }
            Selection newSelection = newSelectionContext != null ? newSelectionContext.getSelection() : Selection.EMPTY;
            boolean selectionChange = !oldSelection.equals(newSelection);
            this.selectionContext = newSelectionContext;
            this.selection = newSelection;
            SelectionChangeEvent changeEvent = selectionChangeSupport.createEvent(this.selectionContext,
                                                                                  this.selection);
            selectionChangeSupport.fireSelectionContextChange(changeEvent);
            if (selectionChange) {
                selectionChangeSupport.fireSelectionChange(changeEvent);
            }
            if (this.selectionContext != null) {
                this.selectionContext.addSelectionChangeListener(selectionChangeMulticaster);
            }
        }
    }

    @Override
    public Selection getSelection() {
        return selection;
    }

    @Override
    public void addSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.addSelectionChangeListener(listener);
    }

    @Override
    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        selectionChangeSupport.removeSelectionChangeListener(listener);
    }

    @Override
    public SelectionChangeListener[] getSelectionChangeListeners() {
        return selectionChangeSupport.getSelectionChangeListeners();
    }

    private class SelectionChangeMulticaster implements SelectionChangeListener {

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            if (isAcceptedEvent(event)) {
                DefaultSelectionManager.this.selection = event.getSelection();
                selectionChangeSupport.fireSelectionChange(event);
            }
        }

        @Override
        public void selectionContextChanged(SelectionChangeEvent event) {
            if (isAcceptedEvent(event)) {
                selectionChangeSupport.fireSelectionContextChange(event);
            }
        }

        private boolean isAcceptedEvent(SelectionChangeEvent event) {
            return event.getSelectionContext() == getSelectionContext();
        }
    }
}