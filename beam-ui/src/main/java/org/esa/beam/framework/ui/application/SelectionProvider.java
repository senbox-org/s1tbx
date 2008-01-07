package org.esa.beam.framework.ui.application;

/**
 * Interface common to all objects that provide a selection.
 */
public interface SelectionProvider {

    /**
     * @return The current selection.
     */
    Selection getSelection();

    /**
     * @param selection The current selection.
     */
    void setSelection(Selection selection);

    /**
     * Adds the given selection listener.
     *
     * @param listener The selection listener.
     */
    void addSelectionChangeListener(SelectionChangeListener listener);

    /**
     * Removes the given selection listener.
     *
     * @param listener The selection listener.
     */
    void removeSelectionChangeListener(SelectionChangeListener listener);
}
