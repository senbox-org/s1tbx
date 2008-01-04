package org.esa.beam.framework.ui.application;

/**
 * The service keeps track of the selection in the currently active part and propagates
 * selection changes to all registered listeners. Such selection events occur when the
 * selection in the current part is changed or when a different part is activated.
 * Both can be triggered by user interaction or programmatically.
 * <p>This interface is not intended to be implemented by clients.</p>
 */
public interface SelectionService {
    /**
     * @return the current selection in the active part.
     */
    Selection getSelection();

    /**
     * @param partId The identifier of the UI-part.
     * @return the current selection in the part with the given identifier.
     */
    Selection getSelection(String partId);

    /**
     * Adds the given selection listener.
     *
     * @param listener The selection listener.
     */
    void addSelectionListener(SelectionListener listener);

    /**
     * Adds a part-specific selection listener which is notified when selection changes in the part with the given identifier.
     *
     * @param partId   The identifier of the UI-part.
     * @param listener The selection listener.
     */
    void addSelectionListener(String partId, SelectionListener listener);

    /**
     * Removes the given selection listener.
     *
     * @param listener The selection listener.
     */
    void removeSelectionListener(SelectionListener listener);

    /**
     * Removes the given part-specific selection listener.
     *
     * @param partId   The identifier of the UI-part.
     * @param listener The selection listener.
     */
    void removeSelectionListener(String partId, SelectionListener listener);
}
