package org.esa.beam.framework.ui.application;

/**
 * Interface for listening to selection changes.
 * This interface may be implemented by clients.
 */
public interface SelectionListener {
    /**
     * Notifies this listener that the selection has changed.
     *
     * @param pageComponent The source page component.
     * @param selection     The current selection.
     */
    void selectionChanged(PageComponent pageComponent, Selection selection);
}
