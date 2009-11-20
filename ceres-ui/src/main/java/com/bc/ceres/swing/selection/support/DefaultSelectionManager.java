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
        this.selectionContext = NullSelectionContext.INSTANCE;
        this.selection = Selection.EMPTY;
        if (GraphicsEnvironment.isHeadless()) {
            this.clipboard = new Clipboard("HeadlessClipboard");
        }else {
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
    public void setSelectionContext(SelectionContext selectionContext) {
        SelectionContext oldSelectionContext = this.selectionContext;
        Selection oldSelection = this.selection;
        if (oldSelectionContext != selectionContext) {
            oldSelectionContext.removeSelectionChangeListener(selectionChangeMulticaster);
            boolean selectionChange = !oldSelection.equals(selectionContext.getSelection());
            this.selectionContext = selectionContext;
            this.selection = selectionContext.getSelection();
            SelectionChangeEvent changeEvent = selectionChangeSupport.createEvent(this.selectionContext,
                                                                                  this.selection);
            selectionChangeSupport.fireSelectionContextChange(changeEvent);
            if (selectionChange) {
                selectionChangeSupport.fireSelectionChange(changeEvent);
            }
            this.selectionContext.addSelectionChangeListener(selectionChangeMulticaster);
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