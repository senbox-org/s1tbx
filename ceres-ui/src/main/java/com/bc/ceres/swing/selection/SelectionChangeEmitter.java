package com.bc.ceres.swing.selection;

/**
 * Objects implementing this interface emit selection change events to interested
 * {@link SelectionChangeListener selection change listener}s.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface SelectionChangeEmitter {
    /**
     * Adds a selection change listener to this emitter.
     *
     * @param listener The listener.
     */
    void addSelectionChangeListener(SelectionChangeListener listener);

    /**
     * Removes a selection change listener from this emitter.
     *
     * @param listener The listener.
     */
    void removeSelectionChangeListener(SelectionChangeListener listener);

    /**
     * Gets all registered selection change listeners.
     *
     * @return An array containing all registered listeners.
     */
    SelectionChangeListener[] getSelectionChangeListeners();
}