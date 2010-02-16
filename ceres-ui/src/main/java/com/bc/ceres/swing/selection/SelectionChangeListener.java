package com.bc.ceres.swing.selection;

/**
 * Interface implemented by a class interested in hearing about
 * selection change events.
 * <p/>
 * This interface may be implemented by clients.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public interface SelectionChangeListener extends java.util.EventListener {
    /**
     * A selection change occurred.
     *
     * @param event The selection event.
     */
    void selectionChanged(SelectionChangeEvent event);

    /**
     * Called if a selection context change occurred.
     *
     * @param event The selection event.
     */
    void selectionContextChanged(SelectionChangeEvent event);
}
