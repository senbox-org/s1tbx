package org.esa.beam.framework.ui.application;

/**
 * Interface for listening to selection changes.
 * This interface may be implemented by clients.
 */
public interface SelectionChangeListener {
    /**
     * Notifies this listener that the selection has changed.
     *
     * @param event The selection change event.
     */
    void selectionChanged(SelectionChangeEvent event);
}