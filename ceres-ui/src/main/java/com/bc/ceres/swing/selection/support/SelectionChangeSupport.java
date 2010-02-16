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