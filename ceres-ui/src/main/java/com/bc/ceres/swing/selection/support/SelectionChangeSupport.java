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

import java.util.Vector;

/**
 * Supports implementation of {@link com.bc.ceres.swing.selection.SelectionChangeEmitter}s.
 * This class is thread-safe.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class SelectionChangeSupport {
    private final Vector<SelectionChangeListener> selectionListeners;
    private final Object realEventSource;

    public SelectionChangeSupport() {
        this(null);
    }

    public SelectionChangeSupport(Object realEventSource) {
        this.realEventSource = realEventSource != null ? realEventSource : this;
        this.selectionListeners = new Vector<SelectionChangeListener>();
    }

    public Object getRealEventSource() {
        return realEventSource;
    }

    public void addSelectionChangeListener(SelectionChangeListener listener) {
        selectionListeners.add(listener);
    }

    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        selectionListeners.remove(listener);
    }

    public SelectionChangeListener[] getSelectionChangeListeners() {
        return selectionListeners.toArray(new SelectionChangeListener[selectionListeners.size()]);
    }

    public SelectionChangeEvent createEvent(SelectionContext selectionContext, Selection selection) {
        return new SelectionChangeEvent(realEventSource, selectionContext, selection);
    }

    public void fireSelectionChange(SelectionContext selectionContext, Selection selection) {
        fireSelectionChange(createEvent(selectionContext, selection));
    }

    public void fireSelectionChange(SelectionChangeEvent event) {
        for (SelectionChangeListener selectionChangeListener : getSelectionChangeListeners()) {
            selectionChangeListener.selectionChanged(event);
        }
    }

    public void fireSelectionContextChange(SelectionChangeEvent event) {
        for (SelectionChangeListener selectionChangeListener : getSelectionChangeListeners()) {
            selectionChangeListener.selectionContextChanged(event);
        }
    }
}