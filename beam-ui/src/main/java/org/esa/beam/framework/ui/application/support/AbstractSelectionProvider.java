package org.esa.beam.framework.ui.application.support;

import org.esa.beam.framework.ui.application.SelectionProvider;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.esa.beam.framework.ui.application.Selection;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSelectionProvider implements SelectionProvider {
    private final List<SelectionChangeListener> listenerList;

    protected AbstractSelectionProvider() {
        listenerList = new ArrayList<SelectionChangeListener>(3);
    }

    public synchronized void addSelectionChangeListener(SelectionChangeListener listener) {
        listenerList.add(listener);
    }

    public synchronized void removeSelectionChangeListener(SelectionChangeListener listener) {
        listenerList.remove(listener);
    }

    protected void fireSelectionChange(Object source) {
        fireSelectionChange(source, getSelection());
    }

    protected void fireSelectionChange(Object source, Selection selection) {
        final SelectionChangeEvent event = new SelectionChangeEvent(source, selection);
        for (SelectionChangeListener selectionListener : getSelectionChangeListeners()) {
            selectionListener.selectionChanged(event);
        }
    }

    private synchronized SelectionChangeListener[] getSelectionChangeListeners() {
        return listenerList.toArray(new SelectionChangeListener[listenerList.size()]);
    }
}
